package com.xxl.job.admin.controller;

import com.xxl.job.admin.controller.annotation.PermissionLimit;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.thread.JobTriggerPoolHelper;
import com.xxl.job.admin.core.trigger.TriggerTypeEnum;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.GsonTool;
import com.xxl.job.core.util.XxlJobRemotingUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Created by xuxueli on 17/5/10.
 */
@Controller
@RequestMapping("/api")
public class JobApiController {

    @Resource
    private AdminBiz adminBiz;

    @Resource
    private XxlJobGroupDao xxlJobGroupDao;

    @Resource
    private XxlJobService xxlJobService;

    /**
     * api
     *
     * @param uri
     * @param data
     * @return
     */
    @RequestMapping("/{uri}")
    @ResponseBody
    @PermissionLimit(limit=false)
    public ReturnT<String> api(HttpServletRequest request, @PathVariable("uri") String uri, @RequestBody(required = false) String data) {

        // valid
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, HttpMethod not support.");
        }
        if (uri==null || uri.trim().length()==0) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, uri-mapping empty.");
        }
        if (XxlJobAdminConfig.getAdminConfig().getAccessToken()!=null
                && XxlJobAdminConfig.getAdminConfig().getAccessToken().trim().length()>0
                && !XxlJobAdminConfig.getAdminConfig().getAccessToken().equals(request.getHeader(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN))) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "The access token is wrong.");
        }

        // services mapping
        if ("callback".equals(uri)) {
            List<HandleCallbackParam> callbackParamList = GsonTool.fromJson(data, List.class, HandleCallbackParam.class);
            return adminBiz.callback(callbackParamList);
        } else if ("registry".equals(uri)) {
            RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
            return adminBiz.registry(registryParam);
        } else if ("registryRemove".equals(uri)) {
            RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
            return adminBiz.registryRemove(registryParam);
        } else {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, uri-mapping("+ uri +") not found.");
        }

    }

    @RequestMapping("/addJob")
    @ResponseBody
    @PermissionLimit(limit = false)
    public ReturnT<String> addJobInfo(@RequestBody XxlJobInfo jobInfo) {
        return xxlJobService.add(jobInfo);
    }

    @RequestMapping("/updateJob")
    @ResponseBody
    @PermissionLimit(limit = false)
    public ReturnT<String> updateJob(@RequestBody XxlJobInfo jobInfo) {
        return xxlJobService.update(jobInfo);
    }

    @RequestMapping("/removeJob")
    @ResponseBody
    @PermissionLimit(limit = false)
    public ReturnT<String> removeJob(@RequestBody XxlJobInfo jobInfo) {
        return xxlJobService.remove(jobInfo.getId());
    }

    @RequestMapping("/pauseJob")
    @ResponseBody
    @PermissionLimit(limit = false)
    public ReturnT<String> pauseJob(@RequestBody XxlJobInfo jobInfo) {
        return xxlJobService.stop(jobInfo.getId());
    }

    @RequestMapping("/startJob")
    @ResponseBody
    @PermissionLimit(limit = false)
    public ReturnT<String> startJob(@RequestBody XxlJobInfo jobInfo) {
        return xxlJobService.start(jobInfo.getId());
    }

    @RequestMapping("/trigger")
    @ResponseBody
    @PermissionLimit(limit = false)
    public ReturnT<String> triggerJob(@RequestBody XxlJobInfo jobInfo) {
        // force cover job param
        if (jobInfo.getExecutorParam() == null) {
            jobInfo.setExecutorParam("");
        }

        /**
         * addressList 可扩展该参数
         */
        JobTriggerPoolHelper.trigger(jobInfo.getId(), TriggerTypeEnum.MANUAL, -1, null, jobInfo.getExecutorParam(), null);
        return ReturnT.SUCCESS;
    }

    @RequestMapping("/getGroupId")
    @ResponseBody
    @PermissionLimit(limit = false)
    public ReturnT<String> getGroupId(@RequestBody XxlJobGroup jobGroup) {
        XxlJobGroup group = xxlJobGroupDao.findByName(jobGroup.getAppname());
        if(group == null){
            return new ReturnT<>();
        }
        return new ReturnT<>(String.valueOf(group.getId()));
    }

}
