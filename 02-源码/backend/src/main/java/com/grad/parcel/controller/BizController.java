package com.grad.parcel.controller;

import com.grad.parcel.common.PageResult;
import com.grad.parcel.common.R;
import com.grad.parcel.common.UserContext;
import com.grad.parcel.service.BizService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 驿站业务接口。
 *
 * 权限约定：除 /api/auth/** 外全部需要 JWT；写接口在 Service 内再次校验状态机与数据范围，
 * 菜单/按钮权限只影响前端可见性，不作为服务端授权依据（见需求文档“权限与消息规则”）。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BizController {

    private final BizService biz;

    // ---------------- 看板与下拉 ----------------
    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard(@RequestParam(required = false, defaultValue = "30d") String period) {
        return R.ok(biz.dashboard(period));
    }

    @GetMapping("/options")
    public R<Map<String, Object>> options() {
        return R.ok(biz.options());
    }

    // ---------------- 快件包裹 ----------------
    @GetMapping("/parcel/page")
    public R<PageResult<Map<String, Object>>> parcelPage(
            @RequestParam(required = false) String waybillNo,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String shelfNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Integer minDays,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.parcelPage(waybillNo, companyId, shelfNo, status, dateFrom, dateTo, minDays, keyword, page, size));
    }

    @GetMapping("/parcel/{id}")
    public R<Map<String, Object>> parcelDetail(@PathVariable Long id) {
        return R.ok(biz.parcelDetail(id));
    }

    @PostMapping("/parcel")
    public R<Map<String, Object>> saveParcel(@RequestBody Map<String, Object> body) {
        return R.ok(biz.saveParcel(body));
    }

    @PostMapping("/parcel/{id}/transition")
    public R<Map<String, Object>> transition(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok(biz.transition(id, String.valueOf(body.get("target")), (String) body.get("reason")));
    }

    /** 创新点：执行滞留风险与货架位置推荐，返回六因子明细 */
    @PostMapping("/parcel/{id}/evaluate")
    public R<Map<String, Object>> evaluate(@PathVariable Long id) {
        return R.ok(biz.evaluate(id));
    }

    @GetMapping("/parcel/export")
    public R<Map<String, Object>> export(@RequestParam(required = false) String status,
                                         @RequestParam(required = false) String dateFrom,
                                         @RequestParam(required = false) String dateTo) {
        return R.ok(biz.exportParcels(status, dateFrom, dateTo));
    }

    // ---------------- 入库台 ----------------
    @GetMapping("/intake/desk")
    public R<Map<String, Object>> intakeDesk() {
        return R.ok(biz.intakeDesk());
    }

    // ---------------- 批次交接 ----------------
    @GetMapping("/batch/page")
    public R<PageResult<Map<String, Object>>> batchPage(@RequestParam(required = false) Long courierId,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String dateFrom,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.batchPage(courierId, status, dateFrom, page, size));
    }

    @GetMapping("/batch/{id}")
    public R<Map<String, Object>> batchDetail(@PathVariable Long id) {
        return R.ok(biz.batchDetail(id));
    }

    @PostMapping("/batch")
    public R<Long> saveBatch(@RequestBody Map<String, Object> body) {
        return R.ok(biz.saveBatch(body));
    }

    // ---------------- 货架与站点 ----------------
    @GetMapping("/shelf/page")
    public R<PageResult<Map<String, Object>>> shelfPage(@RequestParam(required = false) String areaCode,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.shelfPage(areaCode, status, page, size));
    }

    @GetMapping("/shelf/map")
    public R<Map<String, Object>> shelfMap() {
        return R.ok(biz.shelfMap());
    }

    @GetMapping("/station/list")
    public R<List<Map<String, Object>>> stations() {
        return R.ok(biz.stationList());
    }

    @GetMapping("/company/list")
    public R<List<Map<String, Object>>> companies() {
        return R.ok(biz.companyList());
    }

    @GetMapping("/courier/page")
    public R<PageResult<Map<String, Object>>> courierPage(@RequestParam(required = false) Long companyId,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.courierPage(companyId, page, size));
    }

    @GetMapping("/staff/page")
    public R<PageResult<Map<String, Object>>> staffPage(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.staffPage(page, size));
    }

    // ---------------- 通知 ----------------
    @GetMapping("/notify/page")
    public R<PageResult<Map<String, Object>>> notifyPage(@RequestParam(required = false) String status,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.notifyPage(status, page, size));
    }

    @PostMapping("/notify/{parcelId}")
    public R<Void> notifyParcel(@PathVariable Long parcelId, @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body == null ? Map.of() : body;
        biz.notifyParcel(parcelId, String.valueOf(b.getOrDefault("notifyType", "站内消息")),
                String.valueOf(b.getOrDefault("result", "已通知")), (String) b.get("content"));
        return R.ok();
    }

    // ---------------- 取件 ----------------
    @GetMapping("/pickup/query")
    public R<List<Map<String, Object>>> pickupQuery(@RequestParam String key) {
        return R.ok(biz.pickupQuery(key));
    }

    @PostMapping("/pickup/confirm")
    public R<Map<String, Object>> confirmPickup(@RequestBody Map<String, Object> body) {
        return R.ok(biz.confirmPickup(body));
    }

    @GetMapping("/pickup/page")
    public R<PageResult<Map<String, Object>>> pickupPage(@RequestParam(required = false) String dateFrom,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.pickupPage(dateFrom, page, size));
    }

    // ---------------- 滞留预警 ----------------
    @GetMapping("/retention/page")
    public R<PageResult<Map<String, Object>>> retentionPage(@RequestParam(required = false) String level,
                                                            @RequestParam(required = false) String handleStatus,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.retentionPage(level, handleStatus, page, size));
    }

    @PostMapping("/retention/generate")
    public R<Map<String, Object>> generateRetention() {
        return R.ok(biz.generateRetention());
    }

    @PostMapping("/retention/notify")
    public R<Map<String, Object>> retentionNotify(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) body.get("ids");
        List<Long> ids = raw == null ? List.of() : raw.stream().map(o -> Long.valueOf(String.valueOf(o))).toList();
        return R.ok(biz.retentionNotify(ids, (String) body.get("note")));
    }

    @PutMapping("/retention/{id}/handle")
    public R<Void> handleRetention(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        biz.handleRetention(id, String.valueOf(body.getOrDefault("handleStatus", "已处理")), (String) body.get("note"));
        return R.ok();
    }

    // ---------------- 退件 ----------------
    @GetMapping("/return/page")
    public R<PageResult<Map<String, Object>>> returnPage(@RequestParam(required = false) String status,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.returnPage(status, page, size));
    }

    @PostMapping("/return")
    public R<Long> createReturn(@RequestBody Map<String, Object> body) {
        return R.ok(biz.createReturn(body));
    }

    @PutMapping("/return/{id}/approve")
    public R<Void> approveReturn(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        biz.approveReturn(id, Boolean.parseBoolean(String.valueOf(body.getOrDefault("pass", "true"))), (String) body.get("reason"));
        return R.ok();
    }

    @GetMapping("/return/handover/page")
    public R<PageResult<Map<String, Object>>> handoverPage(@RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.handoverPage(page, size));
    }

    @PostMapping("/return/handover")
    public R<Void> handoverReturn(@RequestBody Map<String, Object> body) {
        biz.handoverReturn(body);
        return R.ok();
    }

    // ---------------- 异常件 ----------------
    @GetMapping("/exception/page")
    public R<PageResult<Map<String, Object>>> exceptionPage(@RequestParam(required = false) String status,
                                                            @RequestParam(required = false) String exType,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.exceptionPage(status, exType, page, size));
    }

    @PostMapping("/exception")
    public R<Long> createException(@RequestBody Map<String, Object> body) {
        return R.ok(biz.createException(body));
    }

    @PutMapping("/exception/{id}/audit")
    public R<Void> auditException(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        biz.auditException(id, (String) body.get("result"), (String) body.get("handleProcess"));
        return R.ok();
    }

    // ---------------- 盘点与移库 ----------------
    @GetMapping("/stocktake/page")
    public R<PageResult<Map<String, Object>>> stocktakePage(@RequestParam(required = false) String status,
                                                            @RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.stocktakePage(status, page, size));
    }

    @GetMapping("/stocktake/{id}")
    public R<Map<String, Object>> stocktakeDetail(@PathVariable Long id) {
        return R.ok(biz.stocktakeDetail(id));
    }

    @PostMapping("/stocktake")
    public R<Long> createStocktake(@RequestBody Map<String, Object> body) {
        return R.ok(biz.createStocktake(body));
    }

    @PutMapping("/stocktake/item/{id}/check")
    public R<Void> checkItem(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        biz.checkStocktakeItem(id, body.get("actualCount") == null ? 1 : Integer.parseInt(String.valueOf(body.get("actualCount"))),
                String.valueOf(body.getOrDefault("diffType", "无差异")), (String) body.get("remark"));
        return R.ok();
    }

    @PutMapping("/stocktake/{id}/finish")
    public R<Void> finishStocktake(@PathVariable Long id) {
        biz.finishStocktake(id);
        return R.ok();
    }

    @GetMapping("/move/page")
    public R<PageResult<Map<String, Object>>> movePage(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.movePage(page, size));
    }

    @PostMapping("/move")
    public R<Map<String, Object>> moveParcels(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) body.get("parcelIds");
        List<Long> ids = raw == null ? List.of() : raw.stream().map(o -> Long.valueOf(String.valueOf(o))).toList();
        return R.ok(biz.moveParcels(ids, Long.valueOf(String.valueOf(body.get("toShelfId"))), (String) body.get("reason")));
    }

    // ---------------- 评价 ----------------
    @GetMapping("/review/page")
    public R<PageResult<Map<String, Object>>> reviewPage(@RequestParam(required = false) String status,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.reviewPage(status, page, size));
    }

    @PutMapping("/review/{id}/reply")
    public R<Void> replyReview(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        biz.replyReview(id, (String) body.get("reply"));
        return R.ok();
    }

    // ---------------- 创新点：评分与规则 ----------------
    @GetMapping("/score/page")
    public R<PageResult<Map<String, Object>>> scorePage(@RequestParam(required = false) String grade,
                                                        @RequestParam(required = false) String waybill,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.scorePage(grade, waybill, page, size));
    }

    @PutMapping("/score/{id}/adjust")
    public R<Void> adjustScore(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        biz.adjustScore(id, String.valueOf(body.get("grade")), (String) body.get("reason"));
        return R.ok();
    }

    @GetMapping("/rule/page")
    public R<PageResult<Map<String, Object>>> rulePage(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return R.ok(biz.rulePage(page, size));
    }

    @PostMapping("/rule")
    public R<Void> saveRule(@RequestBody Map<String, Object> body) {
        biz.saveRule(body);
        return R.ok();
    }

    /** 当前登录人信息（个人中心右上角头像同步用） */
    @GetMapping("/me/profile")
    public R<Map<String, Object>> profile() {
        return R.ok(Map.of("userId", UserContext.userId(), "realName", UserContext.realName(),
                "roleCode", UserContext.roleCode()));
    }
}
