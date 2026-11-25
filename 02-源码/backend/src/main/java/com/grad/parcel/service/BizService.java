package com.grad.parcel.service;

import com.grad.parcel.common.BizException;
import com.grad.parcel.common.PageResult;
import com.grad.parcel.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 驿站业务服务。
 *
 * 设计要点：
 * 1. 列表查询统一走 {@link #page} 助手，SQL 别名由 {@link #row} 自动 snake_case → camelCase，
 *    前端所有业务列表与系统管理字段风格一致，不再区分直出 Map 的命名。
 * 2. 状态流转、取件、退件审批等写操作全部由后端重新校验（角色、状态机、唯一性），
 *    前端按钮隐藏只是交互层面，不作为权限依据。
 * 3. 创新点「滞留风险与货架位置推荐」的计算、留痕、人工修正集中在 evaluate / adjustScore，
 *    权重来自 rule_config 生效版本，计算快照写入 score_record，原始结果不可覆盖。
 */
@Service
@RequiredArgsConstructor
public class BizService {

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate njdbc;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================================================================
    // 通用查询助手
    // ==================================================================

    /** 结果集 → 驼峰 Map，时间统一格式化为 yyyy-MM-dd HH:mm:ss */
    private Map<String, Object> row(ResultSet rs) throws java.sql.SQLException {
        ResultSetMetaData md = rs.getMetaData();
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            String label = md.getColumnLabel(i);
            Object v = rs.getObject(i);
            if (v instanceof Timestamp ts) {
                v = ts.toLocalDateTime().format(DTF);
            } else if (v instanceof java.sql.Date d) {
                v = d.toLocalDate().toString();
            }
            m.put(camel(label), v);
        }
        return m;
    }

    private String camel(String s) {
        if (s == null || s.indexOf('_') < 0) return s;
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char c : s.toCharArray()) {
            if (c == '_') { up = true; continue; }
            sb.append(up ? Character.toUpperCase(c) : c);
            up = false;
        }
        return sb.toString();
    }

    /**
     * 通用分页。select 只写字段列表，from 写表与 join，where 写条件（不含 WHERE 关键字）。
     * 列名均为代码内硬编码，用户输入一律走命名参数，杜绝注入。
     */
    private PageResult<Map<String, Object>> page(String select, String from, String where,
                                                 MapSqlParameterSource ps, int page, int size,
                                                 String orderBy) {
        String w = (where == null || where.isBlank()) ? "" : " WHERE " + where;
        Long total = njdbc.queryForObject("SELECT COUNT(*) FROM " + from + w, ps, Long.class);
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 200);
        List<Map<String, Object>> rows = njdbc.query(
                "SELECT " + select + " FROM " + from + w + " ORDER BY " + orderBy
                        + " LIMIT " + s + " OFFSET " + (long) (p - 1) * s,
                ps, (rs, i) -> row(rs));
        return PageResult.of(total == null ? 0 : total, p, s, rows);
    }

    private MapSqlParameterSource ps() {
        return new MapSqlParameterSource();
    }

    private void putLike(MapSqlParameterSource ps, String key, String val) {
        if (val != null && !val.isBlank()) ps.addValue(key, "%" + val.trim() + "%");
    }

    private boolean has(String v) {
        return v != null && !v.isBlank();
    }

    // ==================================================================
    // 看板
    // ==================================================================

    /** 首页指标与图表：入库量/出库量/在库/滞留/异常/货架利用率 + 6 组图表 */
    public Map<String, Object> dashboard(String period) {
        Map<String, Object> r = new LinkedHashMap<>();
        // 站点范围：负责人与员工只看本站，管理员看全量（单站点场景下仍按数据范围过滤）
        Long stationId = myStationId();
        String st = stationId == null ? "" : " AND station_id = " + stationId;

        r.put("todayIn", one("SELECT COUNT(*) FROM parcel WHERE DATE(arrive_time)=CURDATE()" + st));
        r.put("todayOut", one("SELECT COUNT(*) FROM parcel WHERE DATE(pickup_time)=CURDATE()" + st));
        r.put("inStock", one("SELECT COUNT(*) FROM parcel WHERE status IN ('在库','已通知')" + st));
        r.put("retention", one("SELECT COUNT(*) FROM parcel WHERE status IN ('在库','已通知') AND retention_days >= 5" + st));
        r.put("exception", one("SELECT COUNT(*) FROM exception_parcel WHERE status <> '已关闭'"));
        r.put("avgDays", dec("SELECT IFNULL(ROUND(AVG(retention_days),1),0) FROM parcel WHERE status IN ('在库','已通知')" + st));
        // 货架利用率：已用 / 总容量
        r.put("shelfRate", dec("SELECT IFNULL(ROUND(SUM(used_count)*100/SUM(capacity),1),0) FROM shelf WHERE status<>'停用'"
                + (stationId == null ? "" : " AND station_id=" + stationId)));

        // 图 1：近 14 天入出库趋势
        r.put("trend", jdbc.queryForList("""
                SELECT d.dt AS dt,
                       (SELECT COUNT(*) FROM parcel p WHERE DATE(p.arrive_time)=d.dt%s) AS inCnt,
                       (SELECT COUNT(*) FROM parcel p WHERE DATE(p.pickup_time)=d.dt%s) AS outCnt
                FROM (SELECT DATE_SUB(CURDATE(), INTERVAL n DAY) dt FROM
                      (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
                       UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12
                       UNION SELECT 13) t) d
                ORDER BY d.dt
                """.formatted(st.replace("station_id", "p.station_id"), st.replace("station_id", "p.station_id"))));
        // 图 2：快递公司件量 TOP6
        r.put("company", jdbc.queryForList("""
                SELECT c.company_name AS name, COUNT(p.id) AS value
                FROM express_company c LEFT JOIN parcel p ON p.company_id=c.id
                GROUP BY c.id, c.company_name ORDER BY value DESC LIMIT 6"""));
        // 图 3：在库时长分布
        r.put("duration", jdbc.queryForList("""
                SELECT CASE WHEN retention_days<=1 THEN '1天内' WHEN retention_days<=3 THEN '2-3天'
                            WHEN retention_days<=5 THEN '4-5天' WHEN retention_days<=9 THEN '6-9天'
                            ELSE '10天以上' END AS name, COUNT(*) AS value
                FROM parcel WHERE status IN ('在库','已通知') GROUP BY name
                ORDER BY FIELD(name,'1天内','2-3天','4-5天','6-9天','10天以上')"""));
        // 图 4：时段取件量
        // 先聚合再套标签：only_full_group_by 下不允许 SELECT 非聚合列
        r.put("hour", jdbc.queryForList("""
                SELECT CONCAT(LPAD(t.h,2,'0'),':00') AS name, t.c AS value FROM
                (SELECT HOUR(pickup_time) h, COUNT(*) c FROM parcel WHERE pickup_time IS NOT NULL
                 GROUP BY HOUR(pickup_time)) t ORDER BY t.h"""));
        // 图 5：异常类型
        r.put("exType", jdbc.queryForList("""
                SELECT ex_type AS name, COUNT(*) AS value FROM exception_parcel
                GROUP BY ex_type ORDER BY value DESC"""));
        // 图 6：员工处理量 TOP6
        r.put("staff", jdbc.queryForList("""
                SELECT u.real_name AS name, COUNT(p.id) AS value FROM parcel p
                JOIN sys_user u ON u.id=p.handler_id GROUP BY u.id, u.real_name
                ORDER BY value DESC LIMIT 6"""));
        // 图 7：评分结果分布（创新模块效果统计）
        r.put("grade", jdbc.queryForList("""
                SELECT grade AS name, COUNT(*) AS value FROM score_record GROUP BY grade"""));
        r.put("adjustRate", dec("""
                SELECT IFNULL(ROUND(SUM(CASE WHEN manual_adjust IS NOT NULL AND manual_adjust<>'' THEN 1 ELSE 0 END)*100/COUNT(*),1),0)
                FROM score_record"""));
        return r;
    }

    private Long myStationId() {
        // 员工 / 负责人 绑定在 sys_user.org_id，而站点表通过 manager_id 关联：
        // 这里优先取该用户负责的站点，其次取 org 对应的站点
        Long uid = UserContext.userId();
        if (uid == null || UserContext.isAdmin()) return null;
        List<Long> ids = jdbc.queryForList("SELECT id FROM station WHERE manager_id=? AND deleted=0", Long.class, uid);
        if (!ids.isEmpty()) return ids.get(0);
        Long sid = UserContext.orgId();
        return sid != null && sid <= 3 ? sid : null;
    }

    private long one(String sql) {
        Long v = jdbc.queryForObject(sql, Long.class);
        return v == null ? 0 : v;
    }

    private BigDecimal dec(String sql) {
        BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }

    // ==================================================================
    // 快件包裹
    // ==================================================================

    /** 快件列表：支持运单号/公司/货架/状态/入库日期/滞留天数筛选 */
    public PageResult<Map<String, Object>> parcelPage(String waybillNo, Long companyId, String shelfNo,
                                                      String status, String dateFrom, String dateTo,
                                                      Integer minDays, String keyword, int page, int size) {
        StringBuilder w = new StringBuilder("p.deleted=0");
        MapSqlParameterSource ps = ps();
        if (has(waybillNo)) { w.append(" AND p.waybill_no LIKE :wb"); putLike(ps, "wb", waybillNo); }
        if (has(keyword)) {
            w.append(" AND (p.waybill_no LIKE :kw OR p.receiver_name LIKE :kw OR p.pickup_code LIKE :kw)");
            putLike(ps, "kw", keyword);
        }
        if (companyId != null) { w.append(" AND p.company_id=:cid"); ps.addValue("cid", companyId); }
        if (has(shelfNo)) { w.append(" AND p.shelf_no=:sno"); ps.addValue("sno", shelfNo); }
        if (has(status)) { w.append(" AND p.status=:st"); ps.addValue("st", status); }
        if (has(dateFrom)) { w.append(" AND p.arrive_time >= :df"); ps.addValue("df", dateFrom + " 00:00:00"); }
        if (has(dateTo)) { w.append(" AND p.arrive_time <= :dt"); ps.addValue("dt", dateTo + " 23:59:59"); }
        if (minDays != null) { w.append(" AND p.retention_days >= :md"); ps.addValue("md", minDays); }
        if (UserContext.isCustomer()) {
            // 收件人只看本人（按手机号或姓名匹配）
            w.append(" AND (p.receiver_name = :me OR p.receiver_phone = :mePhone)");
            String realName = UserContext.realName();
            ps.addValue("me", realName);
            ps.addValue("mePhone", jdbc.queryForObject(
                    "SELECT phone FROM sys_user WHERE id=?", String.class, UserContext.userId()));
        }
        return page("""
                p.id, p.waybill_no, p.receiver_name, p.receiver_phone, p.phone_tail, p.item_type, p.special_flag,
                p.shelf_no, p.pickup_code, p.status, p.arrive_time, p.shelf_time, p.notify_count,
                p.last_notify_time, p.pickup_time, p.retention_days, p.cover_image, p.remark,
                c.company_name, u.real_name AS handler_name
                """,
                "parcel p LEFT JOIN express_company c ON c.id=p.company_id LEFT JOIN sys_user u ON u.id=p.handler_id",
                w.toString(), ps, page, size, "p.arrive_time DESC, p.id DESC");
    }

    /** 快件详情：主档 + 关联单据 + 时间轴 + 最近一次评分 */
    public Map<String, Object> parcelDetail(Long id) {
        List<Map<String, Object>> l = jdbc.query("""
                SELECT p.*, c.company_name, s.shelf_no AS shelf_code, s.area_code, u.real_name AS handler_name
                FROM parcel p LEFT JOIN express_company c ON c.id=p.company_id
                LEFT JOIN shelf s ON s.id=p.shelf_id LEFT JOIN sys_user u ON u.id=p.handler_id
                WHERE p.id=?""", (rs, i) -> row(rs), id);
        if (l.isEmpty()) throw new BizException("快件不存在或已删除");
        Map<String, Object> m = new LinkedHashMap<>(l.get(0));

        m.put("notifies", jdbc.query("""
                SELECT n.*, u.real_name AS operator_name FROM notify_record n
                LEFT JOIN sys_user u ON u.id=n.operator_id WHERE n.parcel_id=? ORDER BY n.notify_time DESC""",
                (rs, i) -> row(rs), id));
        m.put("pickup", jdbc.query("""
                SELECT r.*, u.real_name AS operator_name FROM pickup_record r
                LEFT JOIN sys_user u ON u.id=r.operator_id WHERE r.parcel_id=? ORDER BY r.pickup_time DESC""",
                (rs, i) -> row(rs), id));
        m.put("shelfRecords", jdbc.query("""
                SELECT sr.*, u.real_name AS operator_name FROM shelf_record sr
                LEFT JOIN sys_user u ON u.id=sr.operator_id WHERE sr.parcel_id=? ORDER BY sr.create_time DESC""",
                (rs, i) -> row(rs), id));
        m.put("exceptions", jdbc.query("SELECT * FROM exception_parcel WHERE parcel_id=? AND deleted=0",
                (rs, i) -> row(rs), id));
        m.put("scores", jdbc.query("SELECT * FROM score_record WHERE parcel_id=? ORDER BY create_time DESC",
                (rs, i) -> row(rs), id));
        // 时间轴：由到站、上架、通知、取件、异常合并
        List<Map<String, Object>> tl = new ArrayList<>();
        tl.add(evt("到站入库", strVal(m.get("arriveTime")), "快递员交接后录入站内台账"));
        if (has(strVal(m.get("shelfTime")))) tl.add(evt("货架分配", strVal(m.get("shelfTime")), "上架至 " + strVal(m.get("shelfNo"))));
        for (Map<String, Object> n : castList(m.get("notifies"))) {
            tl.add(evt("取件通知", strVal(n.get("notifyTime")), strVal(n.get("notifyType")) + "：" + strVal(n.get("result"))));
        }
        for (Map<String, Object> p : castList(m.get("pickup"))) {
            tl.add(evt("身份核验出库", strVal(p.get("pickupTime")), strVal(p.get("pickupType")) + " / " + strVal(p.get("verifyType"))));
        }
        m.put("timeline", tl);
        return m;
    }

    private Map<String, Object> evt(String title, String time, String desc) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("title", title);
        e.put("time", time);
        e.put("desc", desc);
        return e;
    }

    private String strVal(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object o) {
        return o instanceof List ? (List<Map<String, Object>>) o : List.of();
    }

    /** 入库登记：校验运单号唯一、生成取件码、推荐货架、写台账与上架记录 */
    @Transactional
    public Map<String, Object> saveParcel(Map<String, Object> body) {
        String waybill = strVal(body.get("waybillNo")).trim();
        if (waybill.isEmpty()) throw new BizException("运单号不能为空");
        if (waybill.length() > 50) throw new BizException("运单号长度不能超过 50 个字符");
        Long id = body.get("id") == null ? null : Long.valueOf(strVal(body.get("id")));

        Long dup = jdbc.query("SELECT id FROM parcel WHERE waybill_no=? AND deleted=0 AND id<>?",
                rs -> rs.next() ? rs.getLong(1) : null, waybill, id == null ? -1L : id);
        if (dup != null) throw new BizException("运单号 " + waybill + " 已在库，重复入库需填写关联说明");

        Long companyId = body.get("companyId") == null ? null : Long.valueOf(strVal(body.get("companyId")));
        String receiver = strVal(body.get("receiverName"));
        if (receiver.isBlank()) throw new BizException("收件人不能为空");
        String phone = strVal(body.get("receiverPhone"));
        String tail = phone.length() >= 4 ? phone.substring(phone.length() - 4) : strVal(body.get("phoneTail"));

        // 货架：优先用前端确认的货架，其次按件型与分区自动推荐
        Long shelfId = body.get("shelfId") == null ? null : Long.valueOf(strVal(body.get("shelfId")));
        Map<String, Object> shelf = null;
        if (shelfId != null) shelf = shelfById(shelfId);
        if (shelf == null) shelf = recommendShelf(strVal(body.get("itemType")), companyId);

        String pickupCode = has(strVal(body.get("pickupCode"))) ? strVal(body.get("pickupCode")) : genPickupCode();

        if (id == null) {
            jdbc.update("""
                    INSERT INTO parcel(waybill_no, company_id, receiver_name, receiver_phone, phone_tail, item_type,
                        size_desc, special_flag, station_id, batch_id, shelf_id, shelf_no, pickup_code, status,
                        arrive_time, shelf_time, notify_count, retention_days, handler_id, remark, create_by,
                        version, create_time, update_time, deleted)
                    VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0,0,?,?,?,1,NOW(),NOW(),0)
                    """, waybill, companyId, receiver, phone, tail, strVal(body.get("itemType")),
                    strVal(body.get("sizeDesc")), body.getOrDefault("specialFlag", "无"),
                    shelf == null ? null : shelf.get("stationId"), body.get("batchId"),
                    shelf == null ? null : shelf.get("id"), shelf == null ? null : shelf.get("shelfNo"),
                    pickupCode, "在库", body.getOrDefault("arriveTime", LocalDateTime.now().format(DTF)),
                    LocalDateTime.now().format(DTF), UserContext.userId(), strVal(body.get("remark")),
                    UserContext.userId());
            id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            if (shelf != null) {
                jdbc.update("UPDATE shelf SET used_count=used_count+1 WHERE id=?", shelf.get("id"));
                jdbc.update("""
                        INSERT INTO shelf_record(parcel_id, action_type, shelf_id, shelf_no, operator_id, remark,
                            create_by, version, create_time, update_time, deleted)
                        VALUES(?,?,?,?,?,?,?,1,NOW(),NOW(),0)""",
                        id, "上架", shelf.get("id"), shelf.get("shelfNo"), UserContext.userId(), "入库自动上架", UserContext.userId());
            }
        } else {
            jdbc.update("""
                    UPDATE parcel SET waybill_no=?, company_id=?, receiver_name=?, receiver_phone=?, phone_tail=?,
                        item_type=?, size_desc=?, special_flag=?, remark=?, version=version+1, update_time=NOW()
                    WHERE id=?""", waybill, companyId, receiver, phone, tail, strVal(body.get("itemType")),
                    strVal(body.get("sizeDesc")), body.getOrDefault("specialFlag", "无"), strVal(body.get("remark")), id);
        }
        return parcelDetail(id);
    }

    private Map<String, Object> shelfById(Long id) {
        List<Map<String, Object>> l = jdbc.query("SELECT * FROM shelf WHERE id=? AND deleted=0",
                (rs, i) -> row(rs), id);
        return l.isEmpty() ? null : l.get(0);
    }

    /** 货位推荐：按剩余容量与分区匹配挑选可用货架（创新点的“推荐货架”来源） */
    private Map<String, Object> recommendShelf(String itemType, Long companyId) {
        String area = null;
        if (companyId != null) {
            List<String> a = jdbc.queryForList("SELECT area_code FROM express_company WHERE id=?", String.class, companyId);
            area = a.isEmpty() ? null : a.get(0);
        }
        String sql = """
                SELECT * FROM shelf WHERE deleted=0 AND status<>'停用' AND used_count < capacity
                AND (shelf_type=? OR shelf_type='通用')%s
                ORDER BY (capacity-used_count) DESC, id LIMIT 1""";
        String areaCond = area == null ? "" : " AND area_code='" + area.replace("'", "") + "'";
        List<Map<String, Object>> l = jdbc.query(sql.formatted(areaCond), (rs, i) -> row(rs),
                itemType == null || itemType.isBlank() ? "通用" : itemType);
        if (l.isEmpty()) {
            l = jdbc.query("SELECT * FROM shelf WHERE deleted=0 AND status<>'停用' AND used_count<capacity ORDER BY (capacity-used_count) DESC LIMIT 1",
                    (rs, i) -> row(rs));
        }
        return l.isEmpty() ? null : l.get(0);
    }

    private String genPickupCode() {
        String code;
        do {
            code = String.valueOf((int) (Math.random() * 900000) + 100000);
        } while (!jdbc.queryForList("SELECT 1 FROM parcel WHERE pickup_code=? AND deleted=0 LIMIT 1", code).isEmpty());
        return code;
    }

    /** 状态流转：按文档状态机校验，非法跳转直接拒绝；每次流转写操作留痕 */
    @Transactional
    public Map<String, Object> transition(Long id, String target, String reason) {
        Map<String, Object> p = parcelDetail(id);
        String cur = strVal(p.get("status"));
        if (!STATUS_FLOW.getOrDefault(cur, Set.of()).contains(target)) {
            throw new BizException("不允许从「" + cur + "」流转到「" + target + "」");
        }
        if (("已退件".equals(target) || "异常件".equals(target) || "待退回".equals(target)) && !has(reason)) {
            throw new BizException("退回、异常与终态操作必须填写原因（不少于 5 个字）");
        }
        if (has(reason) && reason.trim().length() < 5
                && ("已退件".equals(target) || "异常件".equals(target) || "待退回".equals(target))) {
            throw new BizException("原因不少于 5 个字");
        }
        jdbc.update("UPDATE parcel SET status=?, version=version+1, update_time=NOW() WHERE id=?", target, id);
        jdbc.update("""
                INSERT INTO shelf_record(parcel_id, action_type, shelf_no, operator_id, remark,
                    create_by, version, create_time, update_time, deleted)
                VALUES(?,?,?,?,?,?,1,NOW(),NOW(),0)""",
                id, "状态流转", strVal(p.get("shelfNo")), UserContext.userId(),
                cur + " → " + target + (has(reason) ? "：" + reason : ""), UserContext.userId());
        return Map.of("status", target);
    }

    /** 文档状态机：到站入库→货架分配→待取通知→身份核验→出库完成/异常处理 */
    private static final Map<String, Set<String>> STATUS_FLOW = Map.of(
            "待上架", Set.of("在库", "异常件"),
            "在库", Set.of("已通知", "待退回", "异常件"),
            "已通知", Set.of("已取件", "待退回", "异常件"),
            "已取件", Set.of(),
            "待退回", Set.of("已退件", "在库"),
            "已退件", Set.of(),
            "异常件", Set.of("在库", "待退回"));

    // ==================================================================
    // 创新点：滞留风险与货架位置推荐
    // ==================================================================

    private Map<String, Object> activeRule() {
        List<Map<String, Object>> l = jdbc.query(
                "SELECT * FROM rule_config WHERE status='生效中' AND deleted=0 ORDER BY effective_time DESC LIMIT 1",
                (rs, i) -> row(rs));
        if (l.isEmpty()) throw new BizException("未找到生效中的评分规则，请先在规则配置页启用");
        return l.get(0);
    }

    private Map<String, Integer> parseIntJson(String json) {
        Map<String, Integer> m = new LinkedHashMap<>();
        if (json == null) return m;
        java.util.regex.Matcher mt = java.util.regex.Pattern.compile("\"(\\w+)\"\\s*:\\s*(\\d+)").matcher(json);
        while (mt.find()) m.put(mt.group(1), Integer.parseInt(mt.group(2)));
        return m;
    }

    /**
     * 执行评分：六因子加权，写入输入快照、明细得分、总分、等级与推荐理由。
     * 相同输入 + 相同规则版本必得相同结果（纯函数式计算，无随机项）。
     */
    @Transactional
    public Map<String, Object> evaluate(Long parcelId) {
        Map<String, Object> p = parcelDetail(parcelId);
        Map<String, Object> rule = activeRule();
        Map<String, Integer> w = parseIntJson(strVal(rule.get("weightsJson")));
        int sum = w.values().stream().mapToInt(Integer::intValue).sum();
        if (sum != 100) throw new BizException("规则权重合计为 " + sum + "%，必须等于 100% 才能计算");

        String itemType = strVal(p.get("itemType"));
        String shelfNo = strVal(p.get("shelfNo"));
        int days = p.get("retentionDays") == null ? 0 : ((Number) p.get("retentionDays")).intValue();
        int notify = p.get("notifyCount") == null ? 0 : ((Number) p.get("notifyCount")).intValue();
        String special = strVal(p.get("specialFlag"));

        // --- 因子 1：包裹尺寸与货架规格匹配 ---
        Map<String, Object> shelf = shelfNo.isEmpty() ? null : shelfFirst(shelfNo);
        int sizeScore = 95, capacityScore = 88, areaScore = 92;
        if (shelf == null) {
            sizeScore = 45; capacityScore = 40; areaScore = 50;
        } else {
            String st = strVal(shelf.get("shelfType"));
            sizeScore = st.equals("通用") || st.equals(itemType) ? 95 : 68;
            int cap = ((Number) shelf.getOrDefault("capacity", 0)).intValue();
            int used = ((Number) shelf.getOrDefault("usedCount", 0)).intValue();
            double rate = cap == 0 ? 1 : (double) used / cap;
            capacityScore = rate < 0.6 ? 88 : rate < 0.85 ? 66 : 42;
            String area = strVal(shelf.get("areaCode"));
            String companyArea = strVal(p.get("companyName"));
            areaScore = area.isEmpty() ? 60 : 92;
            if (!companyArea.isEmpty() && !area.isEmpty() && !area.equalsIgnoreCase(companyArea)) areaScore = 72;
        }
        // --- 因子 2：入库时长（滞留越久催取优先级越高）---
        int durScore = days <= 3 ? 90 : days <= 5 ? 78 : days <= 9 ? 62 : 45;
        // --- 因子 3：历史通知次数 ---
        int notifyScore = notify == 0 ? 72 : notify == 1 ? 80 : notify == 2 ? 88 : 92;
        // --- 因子 4：特殊件标记 ---
        int specialScore = switch (special) {
            case "生鲜冷藏" -> 92;
            case "贵重件" -> 86;
            case "易碎品" -> 84;
            default -> 72;
        };

        List<Map<String, Object>> factors = new ArrayList<>();
        factors.add(factor("size", "包裹尺寸", w.getOrDefault("size", 20), sizeScore,
                itemType + " " + strVal(p.get("sizeDesc")) + (shelf == null ? "，尚无货架" : "，与 " + shelfNo + " 规格匹配")));
        factors.add(factor("capacity", "货架容量", w.getOrDefault("capacity", 25), capacityScore,
                shelf == null ? "未分配货架，无法评估剩余容量" : shelfNo + " 剩余容量" + (capacityScore >= 80 ? "充足" : capacityScore >= 60 ? "一般" : "紧张")));
        factors.add(factor("area", "快递公司分区", w.getOrDefault("area", 15), areaScore,
                strVal(p.get("companyName")) + " 约定分区 " + (shelf == null ? "未知" : strVal(shelf.get("areaCode")))));
        factors.add(factor("duration", "入库时长", w.getOrDefault("duration", 25), durScore,
                "已入库 " + days + " 天"));
        factors.add(factor("notify", "通知次数", w.getOrDefault("notify", 10), notifyScore,
                "已通知 " + notify + " 次"));
        factors.add(factor("special", "特殊件标记", w.getOrDefault("special", 5), specialScore,
                "特殊件标记：" + special));

        double total = factors.stream().mapToDouble(f -> ((Number) f.get("weighted")).doubleValue()).sum();
        total = BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP).doubleValue();
        String grade = total >= 80 ? "优选" : total >= 60 ? "可用" : "受限";

        Map<String, Object> best = shelfNo.isEmpty() ? recommendShelf(itemType, null) : shelf;
        String recommendShelfNo = best == null ? "无可用货架" : strVal(best.get("shelfNo"));
        StringBuilder text = new StringBuilder("推荐 ").append(recommendShelfNo);
        text.append("；主要影响因素：");
        text.append(factors.stream().max(Comparator.comparingDouble(f -> ((Number) f.get("weighted")).doubleValue()))
                .map(f -> strVal(f.get("label"))).orElse("—"));
        List<String> misses = new ArrayList<>();
        if (capacityScore < 60) misses.add("货架剩余容量不足");
        if (sizeScore < 80) misses.add("件型与货架规格不匹配");
        if (days >= 10) misses.add("在库已超 10 天，需尽快催取或转退件");
        text.append("；未满足条件：").append(misses.isEmpty() ? "无" : String.join("、", misses));

        String inputJson = toJson(Map.of("waybillNo", strVal(p.get("waybillNo")), "itemType", itemType,
                "shelfNo", shelfNo, "retentionDays", days, "notifyCount", notify,
                "specialFlag", special, "company", strVal(p.get("companyName"))));
        jdbc.update("""
                INSERT INTO score_record(parcel_id, rule_id, rule_version, input_json, scores_json, total_score,
                    grade, recommend_shelf, recommend_text, create_by, version, create_time, update_time, deleted)
                VALUES(?,?,?,?,?,?,?,?,?,?,1,NOW(),NOW(),0)""",
                parcelId, rule.get("id"), rule.get("ruleVersion"), inputJson, toJson(factors),
                BigDecimal.valueOf(total), grade, recommendShelfNo, text.toString(), UserContext.userId());
        Long sid = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", sid);
        out.put("totalScore", total);
        out.put("grade", grade);
        out.put("recommendShelf", recommendShelfNo);
        out.put("recommendText", text.toString());
        out.put("factors", factors);
        out.put("ruleVersion", rule.get("ruleVersion"));
        return out;
    }

    private Map<String, Object> shelfFirst(String shelfNo) {
        List<Map<String, Object>> l = jdbc.query("SELECT * FROM shelf WHERE shelf_no=? AND deleted=0 LIMIT 1",
                (rs, i) -> row(rs), shelfNo);
        return l.isEmpty() ? null : l.get(0);
    }

    private Map<String, Object> factor(String key, String label, int weight, int score, String basis) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("factor", key);
        m.put("label", label);
        m.put("weight", weight);
        m.put("score", score);
        m.put("weighted", BigDecimal.valueOf(score * weight / 100.0).setScale(1, RoundingMode.HALF_UP).doubleValue());
        m.put("basis", basis);
        return m;
    }

    /** 极简 JSON 序列化：避免为了几个快照字段引入额外依赖 */
    private String toJson(Object o) {
        StringBuilder sb = new StringBuilder();
        writeJson(sb, o);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private void writeJson(StringBuilder sb, Object o) {
        if (o == null) { sb.append("null"); return; }
        if (o instanceof Map<?, ?> m) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(e.getKey()).append("\":");
                writeJson(sb, e.getValue());
            }
            sb.append('}');
        } else if (o instanceof List<?> l) {
            sb.append('[');
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) sb.append(',');
                writeJson(sb, l.get(i));
            }
            sb.append(']');
        } else if (o instanceof Number || o instanceof Boolean) {
            sb.append(o);
        } else {
            sb.append('"').append(String.valueOf(o).replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        }
    }

    /** 人工修正：只允许追加调整记录，原始计算结果保留不动 */
    @Transactional
    public void adjustScore(Long scoreId, String grade, String reason) {
        if (!has(reason) || reason.trim().length() < 5) throw new BizException("人工调整必须填写原因（不少于 5 个字）");
        if (!List.of("优选", "可用", "受限").contains(grade)) throw new BizException("调整结果取值非法");
        int n = jdbc.update("""
                UPDATE score_record SET manual_adjust=?, adjust_reason=?, adjust_user_id=?, adjust_time=NOW(),
                    version=version+1, update_time=NOW() WHERE id=?""",
                grade, reason, UserContext.userId(), scoreId);
        if (n == 0) throw new BizException("评分记录不存在");
    }

    public PageResult<Map<String, Object>> scorePage(String grade, String waybill, int page, int size) {
        StringBuilder w = new StringBuilder("s.deleted=0");
        MapSqlParameterSource ps = ps();
        if (has(grade)) { w.append(" AND s.grade=:g"); ps.addValue("g", grade); }
        if (has(waybill)) { w.append(" AND p.waybill_no LIKE :wb"); putLike(ps, "wb", waybill); }
        return page("""
                s.*, p.waybill_no, p.receiver_name, p.item_type, p.status AS parcel_status, p.retention_days
                """, "score_record s LEFT JOIN parcel p ON p.id=s.parcel_id",
                w.toString(), ps, page, size, "s.create_time DESC, s.id DESC");
    }

    public PageResult<Map<String, Object>> rulePage(int page, int size) {
        return page("r.*, u.real_name AS operator_name", "rule_config r LEFT JOIN sys_user u ON u.id=r.operator_id",
                "r.deleted=0", ps(), page, size, "r.id DESC");
    }

    /** 保存规则：权重合计必须 100%，旧版本自动归档，新版本记录生效时间 */
    @Transactional
    public void saveRule(Map<String, Object> body) {
        String weights = strVal(body.get("weightsJson"));
        Map<String, Integer> w = parseIntJson(weights);
        int sum = w.values().stream().mapToInt(Integer::intValue).sum();
        if (sum != 100) throw new BizException("六项权重合计必须等于 100%，当前为 " + sum + "%");
        if (w.size() != 6) throw new BizException("六因子权重必须全部配置");
        String ver = strVal(body.get("ruleVersion"));
        if (!has(ver)) {
            Integer max = jdbc.queryForObject(
                    "SELECT IFNULL(MAX(CAST(SUBSTRING(rule_version,2) AS UNSIGNED)),0) FROM rule_config", Integer.class);
            ver = "v" + (max + 1);
        }
        jdbc.update("UPDATE rule_config SET status='已归档', update_time=NOW() WHERE status='生效中'");
        jdbc.update("""
                INSERT INTO rule_config(rule_name, rule_version, weights_json, threshold_json, status,
                    effective_time, operator_id, create_by, version, create_time, update_time, deleted)
                VALUES(?,?,?,?,'生效中',NOW(),?,?,1,NOW(),NOW(),0)""",
                body.getOrDefault("ruleName", "滞留风险与货架位置推荐"), ver, weights,
                body.getOrDefault("thresholdJson", "{\"normal\":3,\"soon\":5,\"overdue\":9,\"return\":10}"),
                UserContext.userId(), UserContext.userId());
    }

    // ==================================================================
    // 批次交接
    // ==================================================================

    public PageResult<Map<String, Object>> batchPage(Long courierId, String status, String dateFrom, int page, int size) {
        StringBuilder w = new StringBuilder("b.deleted=0");
        MapSqlParameterSource ps = ps();
        if (courierId != null) { w.append(" AND b.courier_id=:ci"); ps.addValue("ci", courierId); }
        if (has(status)) { w.append(" AND b.status=:st"); ps.addValue("st", status); }
        if (has(dateFrom)) { w.append(" AND b.arrive_time >= :df"); ps.addValue("df", dateFrom + " 00:00:00"); }
        // 快递员只看本人批次：注意别名，c 是 express_company（公司），cr 才是 courier（快递员）
        if (UserContext.isCourier()) {
            w.append(" AND cr.user_id=:uid"); ps.addValue("uid", UserContext.userId());
        }
        return page("""
                b.*, c.company_name, cr.courier_name, cr.phone AS courier_phone, u.real_name AS handler_name,
                s.station_name
                """,
                """
                handover_batch b LEFT JOIN express_company c ON c.id=b.company_id
                LEFT JOIN courier cr ON cr.id=b.courier_id LEFT JOIN sys_user u ON u.id=b.handler_id
                LEFT JOIN station s ON s.id=b.station_id""",
                w.toString(), ps, page, size, "b.arrive_time DESC, b.id DESC");
    }

    public Map<String, Object> batchDetail(Long id) {
        List<Map<String, Object>> l = jdbc.query("""
                SELECT b.*, c.company_name, cr.courier_name, s.station_name, u.real_name AS handler_name
                FROM handover_batch b LEFT JOIN express_company c ON c.id=b.company_id
                LEFT JOIN courier cr ON cr.id=b.courier_id LEFT JOIN station s ON s.id=b.station_id
                LEFT JOIN sys_user u ON u.id=b.handler_id WHERE b.id=?""", (rs, i) -> row(rs), id);
        if (l.isEmpty()) throw new BizException("交接批次不存在");
        Map<String, Object> m = new LinkedHashMap<>(l.get(0));
        m.put("items", jdbc.query("SELECT * FROM handover_item WHERE batch_id=? AND deleted=0 ORDER BY id",
                (rs, i) -> row(rs), id));
        m.put("parcels", jdbc.query("SELECT id, waybill_no, receiver_name, status FROM parcel WHERE batch_id=? AND deleted=0",
                (rs, i) -> row(rs), id));
        return m;
    }

    /** 新建交接批次：记录交接总数/接收数/拒收数，差异必须写说明 */
    @Transactional
    public Long saveBatch(Map<String, Object> body) {
        int total = num(body.get("totalCount"));
        int received = num(body.get("receivedCount"));
        int rejected = num(body.get("rejectedCount"));
        int diff = total - received - rejected;
        if (diff != 0 && !has(strVal(body.get("diffNote")))) {
            throw new BizException("交接数量存在差异（" + diff + " 件），必须填写差异说明");
        }
        String no = "HB" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        jdbc.update("""
                INSERT INTO handover_batch(batch_no, company_id, courier_id, station_id, arrive_time, total_count,
                    received_count, rejected_count, diff_count, diff_note, status, handler_id, create_by,
                    version, create_time, update_time, deleted)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,1,NOW(),NOW(),0)""",
                no, body.get("companyId"), body.get("courierId"), body.getOrDefault("stationId", 1),
                body.getOrDefault("arriveTime", LocalDateTime.now().format(DTF)), total, received, rejected,
                diff, strVal(body.get("diffNote")), "已确认", UserContext.userId(), UserContext.userId());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private int num(Object o) {
        if (o == null) return 0;
        try { return Integer.parseInt(String.valueOf(o).trim()); } catch (Exception e) { return 0; }
    }

    // ==================================================================
    // 货架与站点
    // ==================================================================

    public PageResult<Map<String, Object>> shelfPage(String areaCode, String status, int page, int size) {
        StringBuilder w = new StringBuilder("s.deleted=0");
        MapSqlParameterSource ps = ps();
        if (has(areaCode)) { w.append(" AND s.area_code=:ac"); ps.addValue("ac", areaCode); }
        if (has(status)) { w.append(" AND s.status=:st"); ps.addValue("st", status); }
        return page("""
                s.*, st.station_name, ROUND(s.used_count*100/s.capacity,1) AS used_rate
                """, "shelf s LEFT JOIN station st ON st.id=s.station_id",
                w.toString(), ps, page, size, "s.area_code, s.shelf_no");
    }

    /** 货架可视图：按区域分组，附占用率与满架提示 */
    public Map<String, Object> shelfMap() {
        List<Map<String, Object>> all = jdbc.query("""
                SELECT s.*, ROUND(s.used_count*100/s.capacity,1) AS used_rate
                FROM shelf s WHERE s.deleted=0 ORDER BY s.area_code, s.shelf_no""", (rs, i) -> row(rs));
        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
        for (Map<String, Object> s : all) {
            groups.computeIfAbsent(strVal(s.get("areaCode")), k -> new ArrayList<>()).add(s);
        }
        long full = all.stream().filter(s -> ((Number) s.getOrDefault("usedCount", 0)).intValue()
                >= ((Number) s.getOrDefault("capacity", 1)).intValue()).count();
        Map<String, Object> r = new LinkedHashMap<>();
        List<Map<String, Object>> areas = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> e : groups.entrySet()) {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("areaCode", e.getKey() == null ? "未分区" : e.getKey());
            a.put("shelves", e.getValue());
            areas.add(a);
        }
        r.put("areas", areas);
        r.put("total", all.size());
        r.put("fullCount", full);
        return r;
    }

    public List<Map<String, Object>> stationList() {
        return jdbc.query("SELECT s.*, u.real_name AS manager_name FROM station s LEFT JOIN sys_user u ON u.id=s.manager_id WHERE s.deleted=0 ORDER BY s.id",
                (rs, i) -> row(rs));
    }

    public List<Map<String, Object>> companyList() {
        return jdbc.query("SELECT * FROM express_company WHERE deleted=0 ORDER BY id", (rs, i) -> row(rs));
    }

    public PageResult<Map<String, Object>> courierPage(Long companyId, int page, int size) {
        StringBuilder w = new StringBuilder("c.deleted=0");
        MapSqlParameterSource ps = ps();
        if (companyId != null) { w.append(" AND c.company_id=:ci"); ps.addValue("ci", companyId); }
        return page("c.*, ec.company_name, st.station_name",
                "courier c LEFT JOIN express_company ec ON ec.id=c.company_id LEFT JOIN station st ON st.id=c.station_id",
                w.toString(), ps, page, size, "c.id");
    }

    public PageResult<Map<String, Object>> staffPage(int page, int size) {
        return page("u.id, u.username, u.real_name, u.phone, u.avatar, u.status, u.last_login_time, r.role_name, o.org_name",
                "sys_user u LEFT JOIN sys_role r ON r.id=u.role_id LEFT JOIN sys_org o ON o.id=u.org_id",
                "u.deleted=0", ps(), page, size, "u.id");
    }

    // ==================================================================
    // 入库台 / 通知 / 取件
    // ==================================================================

    /** 入库台：待上架与今日入库汇总 */
    public Map<String, Object> intakeDesk() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("pending", jdbc.query("""
                SELECT p.*, c.company_name FROM parcel p LEFT JOIN express_company c ON c.id=p.company_id
                WHERE p.deleted=0 AND p.status='待上架' ORDER BY p.arrive_time DESC LIMIT 20""",
                (rs, i) -> row(rs)));
        r.put("todayCount", one("SELECT COUNT(*) FROM parcel WHERE DATE(arrive_time)=CURDATE()"));
        r.put("waitShelf", one("SELECT COUNT(*) FROM parcel WHERE status='待上架'"));
        r.put("shelfFree", one("SELECT IFNULL(SUM(capacity-used_count),0) FROM shelf WHERE status<>'停用'"));
        r.put("batchToday", one("SELECT COUNT(*) FROM handover_batch WHERE DATE(arrive_time)=CURDATE()"));
        return r;
    }

    public PageResult<Map<String, Object>> notifyPage(String status, int page, int size) {
        StringBuilder w = new StringBuilder("n.deleted=0");
        MapSqlParameterSource ps = ps();
        if (has(status)) { w.append(" AND n.result=:r"); ps.addValue("r", status); }
        return page("""
                n.*, p.waybill_no, p.receiver_name, p.retention_days, u.real_name AS operator_name
                """, "notify_record n LEFT JOIN parcel p ON p.id=n.parcel_id LEFT JOIN sys_user u ON u.id=n.operator_id",
                w.toString(), ps, page, size, "n.notify_time DESC, n.id DESC");
    }

    /** 记录一次取件通知：累加通知次数并刷新最后通知时间（不集成短信商，只留台账） */
    @Transactional
    public void notifyParcel(Long parcelId, String type, String result, String content) {
        Map<String, Object> p = parcelDetail(parcelId);
        jdbc.update("""
                INSERT INTO notify_record(parcel_id, notify_type, content, result, operator_id, notify_time,
                    create_by, version, create_time, update_time, deleted)
                VALUES(?,?,?,?,?,NOW(),?,1,NOW(),NOW(),0)""",
                parcelId, type, has(content) ? content : "您的快件已到站，取件码 " + strVal(p.get("pickupCode")),
                has(result) ? result : "已通知", UserContext.userId(), UserContext.userId());
        jdbc.update("""
                UPDATE parcel SET notify_count=notify_count+1, last_notify_time=NOW(),
                    status=CASE WHEN status='在库' THEN '已通知' ELSE status END,
                    version=version+1, update_time=NOW() WHERE id=?""", parcelId);
    }

    /** 取件查询：运单号 / 手机后四位 / 取件码 三选一 */
    public List<Map<String, Object>> pickupQuery(String key) {
        if (!has(key)) return List.of();
        String k = key.trim();
        return jdbc.query("""
                SELECT p.*, c.company_name FROM parcel p LEFT JOIN express_company c ON c.id=p.company_id
                WHERE p.deleted=0 AND p.status IN ('在库','已通知')
                  AND (p.waybill_no=? OR p.phone_tail=? OR p.pickup_code=?)
                ORDER BY p.arrive_time DESC""", (rs, i) -> row(rs), k, k, k);
    }

    /**
     * 取件核验：本人或代取，逐件确认。终态操作，事务内完成状态、拣货记录、货架回收。
     */
    @Transactional
    public Map<String, Object> confirmPickup(Map<String, Object> body) {
        Object idsObj = body.get("parcelIds");
        List<Long> ids = new ArrayList<>();
        if (idsObj instanceof List<?> l) {
            for (Object o : l) ids.add(Long.valueOf(String.valueOf(o)));
        } else if (body.get("parcelId") != null) {
            ids.add(Long.valueOf(strVal(body.get("parcelId"))));
        }
        if (ids.isEmpty()) throw new BizException("请至少选择一件快件");
        String type = strVal(body.getOrDefault("pickupType", "本人取件"));
        String verify = strVal(body.getOrDefault("verifyType", "取件码核验"));
        String person = strVal(body.get("pickupPerson"));
        String phone = strVal(body.get("pickupPhone"));
        int ok = 0;
        List<String> failed = new ArrayList<>();
        for (Long id : ids) {
            Map<String, Object> p = parcelDetail(id);
            String st = strVal(p.get("status"));
            if (!st.equals("在库") && !st.equals("已通知")) {
                failed.add(strVal(p.get("waybillNo")) + "：当前状态「" + st + "」不可取件");
                continue;
            }
            if ("代取".equals(type) && (person.isBlank() || phone.isBlank())) {
                throw new BizException("代取必须填写代取人姓名与联系电话");
            }
            jdbc.update("""
                    INSERT INTO pickup_record(parcel_id, pickup_type, verify_type, pickup_person, pickup_phone,
                        operator_id, pickup_time, remark, create_by, version, create_time, update_time, deleted)
                    VALUES(?,?,?,?,?,?,NOW(),?,?,1,NOW(),NOW(),0)""",
                    id, type, verify, person.isBlank() ? strVal(p.get("receiverName")) : person,
                    phone.isBlank() ? strVal(p.get("receiverPhone")) : phone,
                    UserContext.userId(), strVal(body.get("remark")), UserContext.userId());
            jdbc.update("""
                    UPDATE parcel SET status='已取件', pickup_time=NOW(), version=version+1, update_time=NOW()
                    WHERE id=?""", id);
            // 回收货架占用
            Object shelfId = p.get("shelfId");
            if (shelfId instanceof Number n) {
                jdbc.update("UPDATE shelf SET used_count=GREATEST(used_count-1,0) WHERE id=?", n.longValue());
            }
            ok++;
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", ok);
        r.put("failed", failed);
        return r;
    }

    public PageResult<Map<String, Object>> pickupPage(String dateFrom, int page, int size) {
        StringBuilder w = new StringBuilder("r.deleted=0");
        MapSqlParameterSource ps = ps();
        if (has(dateFrom)) { w.append(" AND r.pickup_time >= :df"); ps.addValue("df", dateFrom + " 00:00:00"); }
        return page("""
                r.*, p.waybill_no, p.receiver_name, u.real_name AS operator_name
                """, "pickup_record r LEFT JOIN parcel p ON p.id=r.parcel_id LEFT JOIN sys_user u ON u.id=r.operator_id",
                w.toString(), ps, page, size, "r.pickup_time DESC, r.id DESC");
    }

    // ==================================================================
    // 滞留预警
    // ==================================================================

    public PageResult<Map<String, Object>> retentionPage(String level, String handleStatus, int page, int size) {
        StringBuilder w = new StringBuilder("w.deleted=0");
        MapSqlParameterSource ps = ps();
        if (has(level)) { w.append(" AND w.level=:lv"); ps.addValue("lv", level); }
        if (has(handleStatus)) { w.append(" AND w.handle_status=:hs"); ps.addValue("hs", handleStatus); }
        return page("""
                w.*, p.waybill_no, p.receiver_name, p.receiver_phone, p.item_type, p.shelf_no, p.status AS parcel_status
                """, "retention_warning w LEFT JOIN parcel p ON p.id=w.parcel_id",
                w.toString(), ps, page, size, "w.retention_days DESC, w.id DESC");
    }

    /** 每日滞留清单生成：按生效阈值分级，已存在则跳过，保证可重复执行 */
    @Transactional
    public Map<String, Object> generateRetention() {
        Map<String, Object> rule = activeRule();
        Map<String, Integer> th = parseIntJson(strVal(rule.get("thresholdJson")));
        int soon = th.getOrDefault("soon", 5);
        int overdue = th.getOrDefault("overdue", 9);
        List<Map<String, Object>> list = jdbc.query("""
                SELECT p.id, p.retention_days, p.notify_count FROM parcel p
                WHERE p.deleted=0 AND p.status IN ('在库','已通知') AND p.retention_days >= ?""",
                (rs, i) -> row(rs), soon);
        int created = 0;
        for (Map<String, Object> p : list) {
            int days = ((Number) p.get("retentionDays")).intValue();
            String level = days >= overdue ? "已滞留" : "即将滞留";
            Long exists = jdbc.query("SELECT id FROM retention_warning WHERE parcel_id=? AND handle_status='待处理' AND deleted=0 LIMIT 1",
                    rs -> rs.next() ? rs.getLong(1) : null, p.get("id"));
            if (exists != null) continue;
            jdbc.update("""
                    INSERT INTO retention_warning(parcel_id, retention_days, level, notify_count, handle_status,
                        create_by, version, create_time, update_time, deleted)
                    VALUES(?,?,?,?,'待处理',?,1,NOW(),NOW(),0)""",
                    p.get("id"), days, level, p.get("notifyCount"), UserContext.userId());
            created++;
        }
        return Map.of("created", created, "scanned", list.size());
    }

    /** 批量二次通知：逐条返回成功/失败，禁止整批静默失败 */
    @Transactional
    public Map<String, Object> retentionNotify(List<Long> ids, String note) {
        int ok = 0;
        List<String> failed = new ArrayList<>();
        for (Long wid : ids) {
            List<Map<String, Object>> l = jdbc.query("SELECT * FROM retention_warning WHERE id=? AND deleted=0",
                    (rs, i) -> row(rs), wid);
            if (l.isEmpty()) { failed.add(wid + "：预警记录不存在"); continue; }
            Long pid = ((Number) l.get(0).get("parcelId")).longValue();
            notifyParcel(pid, "二次通知", "已通知", has(note) ? note : "滞留二次催取通知");
            jdbc.update("""
                    UPDATE retention_warning SET notify_count=notify_count+1, handle_status='已通知',
                        handle_note=?, handle_time=NOW(), handler_id=?, version=version+1, update_time=NOW()
                    WHERE id=?""", note, UserContext.userId(), wid);
            ok++;
        }
        return Map.of("success", ok, "failed", failed);
    }

    @Transactional
    public void handleRetention(Long id, String handleStatus, String note) {
        if (!has(note)) throw new BizException("处理备注不能为空");
        int n = jdbc.update("""
                UPDATE retention_warning SET handle_status=?, handle_note=?, handle_time=NOW(),
                    handler_id=?, version=version+1, update_time=NOW() WHERE id=?""",
                handleStatus, note, UserContext.userId(), id);
        if (n == 0) throw new BizException("预警记录不存在");
    }

    // ==================================================================
    // 退件
    // ==================================================================

    public PageResult<Map<String, Object>> returnPage(String status, int page, int size) {
        StringBuilder w = new StringBuilder("r.deleted=0");
        MapSqlParameterSource ps = ps();
        if (has(status)) { w.append(" AND r.status=:st"); ps.addValue("st", status); }
        return page("""
                r.*, p.receiver_name, p.shelf_no, u.real_name AS apply_user_name
                """, "return_apply r LEFT JOIN parcel p ON p.id=r.parcel_id LEFT JOIN sys_user u ON u.id=r.apply_user_id",
                w.toString(), ps, page, size, "r.create_time DESC, r.id DESC");
    }

    @Transactional
    public Long createReturn(Map<String, Object> body) {
        Long parcelId = Long.valueOf(strVal(body.get("parcelId")));
        Map<String, Object> p = parcelDetail(parcelId);
        String st = strVal(p.get("status"));
        if (!List.of("在库", "已通知").contains(st)) throw new BizException("当前状态「" + st + "」不可发起退件");
        long exists = one("SELECT COUNT(*) FROM return_apply WHERE parcel_id=" + parcelId + " AND status IN ('待审批','待交接')");
        if (exists > 0) throw new BizException("该快件已有进行中的退件申请");
        String no = "RT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        jdbc.update("""
                INSERT INTO return_apply(apply_no, parcel_id, waybill_no, reason_type, reason, apply_user_id,
                    status, create_by, version, create_time, update_time, deleted)
                VALUES(?,?,?,?,?,?,'待审批',?,1,NOW(),NOW(),0)""",
                no, parcelId, p.get("waybillNo"), body.getOrDefault("reasonType", "超期未取"),
                strVal(body.get("reason")), UserContext.userId(), UserContext.userId());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /** 退件审批：负责人确认后进入待交接，同时把快件状态置为待退回 */
    @Transactional
    public void approveReturn(Long id, boolean pass, String reason) {
        if (!pass && (!has(reason) || reason.trim().length() < 5)) throw new BizException("驳回必须填写原因（不少于 5 个字）");
        List<Map<String, Object>> l = jdbc.query("SELECT * FROM return_apply WHERE id=? AND deleted=0", (rs, i) -> row(rs), id);
        if (l.isEmpty()) throw new BizException("退件申请不存在");
        Map<String, Object> r = l.get(0);
        if (!"待审批".equals(strVal(r.get("status")))) throw new BizException("该申请已处理，不能重复审批");
        jdbc.update("""
                UPDATE return_apply SET status=?, approve_user_id=?, approve_time=NOW(),
                    version=version+1, update_time=NOW() WHERE id=?""",
                pass ? "待交接" : "已驳回", UserContext.userId(), id);
        if (pass) {
            jdbc.update("UPDATE parcel SET status='待退回', version=version+1, update_time=NOW() WHERE id=?", r.get("parcelId"));
        }
    }

    public PageResult<Map<String, Object>> handoverPage(int page, int size) {
        return page("""
                h.*, r.apply_no, r.waybill_no, c.courier_name
                """, """
                return_handover h LEFT JOIN return_apply r ON r.id=h.apply_id
                LEFT JOIN courier c ON c.id=h.courier_id""",
                "h.deleted=0", ps(), page, size, "h.id DESC");
    }

    /** 退件交接：数量核对不符必须说明 */
    @Transactional
    public void handoverReturn(Map<String, Object> body) {
        Long applyId = Long.valueOf(strVal(body.get("applyId")));
        int plan = num(body.get("planCount"));
        int actual = num(body.get("actualCount"));
        if (plan != actual && !has(strVal(body.get("remark")))) throw new BizException("交接数量不一致，必须填写说明");
        jdbc.update("""
                INSERT INTO return_handover(apply_id, courier_id, plan_count, actual_count, handover_time,
                    receiver_name, voucher_file, remark, create_by, version, create_time, update_time, deleted)
                VALUES(?,?,?,?,NOW(),?,?,?,?,1,NOW(),NOW(),0)""",
                applyId, body.get("courierId"), plan, actual, strVal(body.get("receiverName")),
                strVal(body.get("voucherFile")), strVal(body.get("remark")), UserContext.userId());
        jdbc.update("UPDATE return_apply SET status='已交接', version=version+1, update_time=NOW() WHERE id=?", applyId);
        jdbc.update("UPDATE parcel p JOIN return_apply r ON r.parcel_id=p.id SET p.status='已退件' WHERE r.id=?", applyId);
    }

    // ==================================================================
    // 异常件
    // ==================================================================

    public PageResult<Map<String, Object>> exceptionPage(String status, String exType, int page, int size) {
        StringBuilder w = new StringBuilder("e.deleted=0");
        MapSqlParameterSource ps = ps();
        if (has(status)) { w.append(" AND e.status=:st"); ps.addValue("st", status); }
        if (has(exType)) { w.append(" AND e.ex_type=:et"); ps.addValue("et", exType); }
        return page("""
                e.*, p.receiver_name, p.shelf_no
                """, "exception_parcel e LEFT JOIN parcel p ON p.id=e.parcel_id",
                w.toString(), ps, page, size, "e.create_time DESC, e.id DESC");
    }

    @Transactional
    public Long createException(Map<String, Object> body) {
        String no = "EX" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        jdbc.update("""
                INSERT INTO exception_parcel(exception_no, parcel_id, waybill_no, ex_type, find_stage, duty_side,
                    description, images, handle_process, result, status, registrar_id, create_by,
                    version, create_time, update_time, deleted)
                VALUES(?,?,?,?,?,?,?,?,?,?,'待处理',?,?,1,NOW(),NOW(),0)""",
                no, body.get("parcelId"), body.get("waybillNo"), body.getOrDefault("exType", "破损"),
                body.getOrDefault("findStage", "入库"), body.getOrDefault("dutySide", "待判定"),
                strVal(body.get("description")), strVal(body.get("images")),
                strVal(body.get("handleProcess")), strVal(body.get("result")),
                UserContext.userId(), UserContext.userId());
        if (body.get("parcelId") != null) {
            jdbc.update("UPDATE parcel SET status='异常件', version=version+1, update_time=NOW() WHERE id=?", body.get("parcelId"));
        }
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /** 异常件审核关闭：丢失与重大破损必须负责人审核，留完整处理链路 */
    @Transactional
    public void auditException(Long id, String result, String process) {
        if (!has(result) || !has(process)) throw new BizException("审核结果与处理过程不能为空");
        int n = jdbc.update("""
                UPDATE exception_parcel SET result=?, handle_process=?, status='已关闭', audit_user_id=?,
                    audit_time=NOW(), version=version+1, update_time=NOW() WHERE id=?""",
                result, process, UserContext.userId(), id);
        if (n == 0) throw new BizException("异常件不存在");
    }

    // ==================================================================
    // 盘点与移库
    // ==================================================================

    public PageResult<Map<String, Object>> stocktakePage(String status, int page, int size) {
        StringBuilder w = new StringBuilder("t.deleted=0");
        MapSqlParameterSource ps = ps();
        if (has(status)) { w.append(" AND t.status=:st"); ps.addValue("st", status); }
        return page("t.*, s.station_name, u.real_name AS operator_name",
                "stocktake_task t LEFT JOIN station s ON s.id=t.station_id LEFT JOIN sys_user u ON u.id=t.operator_id",
                w.toString(), ps, page, size, "t.create_time DESC, t.id DESC");
    }

    public Map<String, Object> stocktakeDetail(Long id) {
        List<Map<String, Object>> l = jdbc.query("SELECT * FROM stocktake_task WHERE id=? AND deleted=0", (rs, i) -> row(rs), id);
        if (l.isEmpty()) throw new BizException("盘点任务不存在");
        Map<String, Object> m = new LinkedHashMap<>(l.get(0));
        m.put("items", jdbc.query("SELECT * FROM stocktake_item WHERE task_id=? AND deleted=0 ORDER BY id", (rs, i) -> row(rs), id));
        return m;
    }

    /** 新建盘点任务：按货架生成明细，系统在库件作为账面数 */
    @Transactional
    public Long createStocktake(Map<String, Object> body) {
        Long shelfId = body.get("shelfId") == null ? null : Long.valueOf(strVal(body.get("shelfId")));
        List<Map<String, Object>> parcels = shelfId == null
                ? jdbc.query("SELECT id, waybill_no, shelf_no FROM parcel WHERE status IN ('在库','已通知') AND deleted=0",
                (rs, i) -> row(rs))
                : jdbc.query("SELECT id, waybill_no, shelf_no FROM parcel WHERE shelf_id=? AND status IN ('在库','已通知') AND deleted=0",
                (rs, i) -> row(rs), shelfId);
        String no = "ST" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        jdbc.update("""
                INSERT INTO stocktake_task(task_no, station_id, shelf_ids, shelf_count, plan_count, actual_count,
                    profit_count, loss_count, mismatch_count, status, operator_id, create_by, version,
                    create_time, update_time, deleted)
                VALUES(?,?,?,1,?,0,0,0,0,'进行中',?,?,1,NOW(),NOW(),0)""",
                no, body.getOrDefault("stationId", 1), shelfId == null ? "ALL" : String.valueOf(shelfId),
                parcels.size(), UserContext.userId(), UserContext.userId());
        Long tid = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        for (Map<String, Object> p : parcels) {
            jdbc.update("""
                    INSERT INTO stocktake_item(task_id, parcel_id, waybill_no, shelf_no, system_count, actual_count,
                        diff_type, handle_status, create_by, version, create_time, update_time, deleted)
                    VALUES(?,?,?,?,1,1,'无差异','已核对',?,1,NOW(),NOW(),0)""",
                    tid, p.get("id"), p.get("waybillNo"), p.get("shelfNo"), UserContext.userId());
        }
        return tid;
    }

    /** 盘点明细核对：差异不能直接删件，只能标记后走异常复核 */
    @Transactional
    public void checkStocktakeItem(Long itemId, int actualCount, String diffType, String remark) {
        List<Map<String, Object>> l = jdbc.query("SELECT * FROM stocktake_item WHERE id=? AND deleted=0", (rs, i) -> row(rs), itemId);
        if (l.isEmpty()) throw new BizException("盘点明细不存在");
        if (!"无差异".equals(diffType) && !has(remark)) throw new BizException("存在差异必须填写复核说明");
        jdbc.update("""
                UPDATE stocktake_item SET actual_count=?, diff_type=?, handle_status='已核对', remark=?,
                    version=version+1, update_time=NOW() WHERE id=?""", actualCount, diffType, remark, itemId);
        Long taskId = ((Number) l.get(0).get("taskId")).longValue();
        // 汇总任务统计
        Map<String, Object> agg = jdbc.queryForMap("""
                SELECT COUNT(*) c, IFNULL(SUM(diff_type='盘盈'),0) p, IFNULL(SUM(diff_type='盘亏'),0) ls,
                       IFNULL(SUM(diff_type='错位'),0) mm
                FROM stocktake_item WHERE task_id=? AND deleted=0""", taskId);
        jdbc.update("""
                UPDATE stocktake_task SET actual_count=?, profit_count=?, loss_count=?, mismatch_count=?,
                    version=version+1, update_time=NOW() WHERE id=?""",
                ((Number) agg.get("c")).intValue(), ((Number) agg.get("p")).intValue(),
                ((Number) agg.get("ls")).intValue(), ((Number) agg.get("mm")).intValue(), taskId);
    }

    @Transactional
    public void finishStocktake(Long id) {
        jdbc.update("UPDATE stocktake_task SET status='已完成', finish_time=NOW(), version=version+1, update_time=NOW() WHERE id=?", id);
    }

    public PageResult<Map<String, Object>> movePage(int page, int size) {
        return page("m.*, p.waybill_no, u.real_name AS operator_name",
                "move_record m LEFT JOIN parcel p ON p.id=m.parcel_id LEFT JOIN sys_user u ON u.id=m.operator_id",
                "m.deleted=0", ps(), page, size, "m.create_time DESC, m.id DESC");
    }

    /** 移库：目标货架必须启用且容量充足，同步两边占用数 */
    @Transactional
    public Map<String, Object> moveParcels(List<Long> parcelIds, Long toShelfId, String reason) {
        if (!has(reason)) throw new BizException("移库必须填写原因");
        Map<String, Object> target = shelfById(toShelfId);
        if (target == null) throw new BizException("目标货架不存在");
        if ("停用".equals(strVal(target.get("status")))) throw new BizException("目标货架已停用");
        int cap = ((Number) target.get("capacity")).intValue();
        int used = ((Number) target.get("usedCount")).intValue();
        if (used + parcelIds.size() > cap) {
            throw new BizException("目标货架剩余容量不足（剩余 " + (cap - used) + "，需 " + parcelIds.size() + "）");
        }
        int ok = 0;
        for (Long pid : parcelIds) {
            Map<String, Object> p = parcelDetail(pid);
            jdbc.update("""
                    INSERT INTO move_record(parcel_id, from_shelf_id, from_shelf_no, to_shelf_id, to_shelf_no,
                        reason, operator_id, create_by, version, create_time, update_time, deleted)
                    VALUES(?,?,?,?,?,?,?,?,1,NOW(),NOW(),0)""",
                    pid, p.get("shelfId"), p.get("shelfNo"), toShelfId, target.get("shelfNo"),
                    reason, UserContext.userId(), UserContext.userId());
            jdbc.update("UPDATE parcel SET shelf_id=?, shelf_no=?, version=version+1, update_time=NOW() WHERE id=?",
                    toShelfId, target.get("shelfNo"), pid);
            if (p.get("shelfId") instanceof Number n) {
                jdbc.update("UPDATE shelf SET used_count=GREATEST(used_count-1,0) WHERE id=?", n.longValue());
            }
            ok++;
        }
        jdbc.update("UPDATE shelf SET used_count=used_count+? WHERE id=?", ok, toShelfId);
        return Map.of("success", ok);
    }

    // ==================================================================
    // 评价
    // ==================================================================

    public PageResult<Map<String, Object>> reviewPage(String status, int page, int size) {
        StringBuilder w = new StringBuilder("r.deleted=0");
        MapSqlParameterSource ps = ps();
        if (has(status)) { w.append(" AND r.status=:st"); ps.addValue("st", status); }
        return page("r.*, p.waybill_no, p.shelf_no", "review r LEFT JOIN parcel p ON p.id=r.parcel_id",
                w.toString(), ps, page, size, "r.create_time DESC, r.id DESC");
    }

    @Transactional
    public void replyReview(Long id, String reply) {
        if (!has(reply)) throw new BizException("回复内容不能为空");
        int n = jdbc.update("""
                UPDATE review SET reply=?, reply_time=NOW(), status='已回复', version=version+1, update_time=NOW()
                WHERE id=?""", reply, id);
        if (n == 0) throw new BizException("评价不存在");
    }

    // ==================================================================
    // 下拉选项（一次取全，减少前端串联请求）
    // ==================================================================

    public Map<String, Object> options() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("companies", companyList());
        m.put("stations", stationList());
        m.put("shelves", jdbc.query("""
                SELECT id, shelf_no, area_code, shelf_type, capacity, used_count FROM shelf
                WHERE deleted=0 AND status<>'停用' ORDER BY area_code, shelf_no""", (rs, i) -> row(rs)));
        m.put("couriers", jdbc.query("SELECT id, courier_name, company_id, phone FROM courier WHERE deleted=0", (rs, i) -> row(rs)));
        m.put("dicts", jdbc.query("SELECT dict_type, dict_label, dict_value FROM sys_dict WHERE status=1 ORDER BY dict_type, sort_no",
                (rs, i) -> row(rs)));
        m.put("parcelStatus", List.of("待上架", "在库", "已通知", "已取件", "待退回", "已退件", "异常件"));
        m.put("itemTypes", List.of("小件", "中件", "大件", "异形件"));
        m.put("specialFlags", List.of("无", "贵重件", "生鲜冷藏", "易碎品"));
        return m;
    }

    /** 导出：按当前筛选返回全量（前端转 CSV），并记录导出任务 */
    @Transactional
    public Map<String, Object> exportParcels(String status, String dateFrom, String dateTo) {
        StringBuilder w = new StringBuilder("p.deleted=0");
        MapSqlParameterSource ps = ps();
        if (has(status)) { w.append(" AND p.status=:st"); ps.addValue("st", status); }
        if (has(dateFrom)) { w.append(" AND p.arrive_time >= :df"); ps.addValue("df", dateFrom + " 00:00:00"); }
        if (has(dateTo)) { w.append(" AND p.arrive_time <= :dt"); ps.addValue("dt", dateTo + " 23:59:59"); }
        List<Map<String, Object>> rows = njdbc.query("""
                SELECT p.waybill_no AS 运单号, c.company_name AS 快递公司, p.receiver_name AS 收件人,
                       p.receiver_phone AS 手机号, p.item_type AS 件型, p.shelf_no AS 货架,
                       p.pickup_code AS 取件码, p.status AS 状态, p.arrive_time AS 入库时间,
                       p.pickup_time AS 取件时间, p.retention_days AS 在库天数
                FROM parcel p LEFT JOIN express_company c ON c.id=p.company_id
                """ + " WHERE " + w + " ORDER BY p.arrive_time DESC", ps,
                (rs, i) -> row(rs));
        jdbc.update("""
                INSERT INTO sys_log(user_id, user_name, module, action, method, params, status, cost_ms, create_time, update_time, deleted)
                VALUES(?,?,?,?,?,?,1,0,NOW(),NOW(),0)""",
                UserContext.userId(), UserContext.realName(), "快件包裹", "导出",
                "GET /api/parcel/export", "status=" + status + ",from=" + dateFrom + ",to=" + dateTo);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rows", rows);
        m.put("count", rows.size());
        m.put("fileName", "快件台账_" + LocalDate.now() + ".csv");
        return m;
    }
}
