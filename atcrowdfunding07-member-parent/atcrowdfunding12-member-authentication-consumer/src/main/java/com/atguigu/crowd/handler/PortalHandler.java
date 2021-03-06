package com.atguigu.crowd.handler;

import com.atguigu.crowd.api.MySQLRemoteService;
import com.atguigu.crowd.constant.CrowdConstant;
import com.atguigu.crowd.entity.vo.PortalTypeVO;
import com.atguigu.crowd.util.ResultEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
public class PortalHandler {

    @Autowired
    private MySQLRemoteService sqlRemoteService;


    @RequestMapping("/")
    public String showPortalPage(Model model) {

        // 这里实际开发中需要加载数据……
        // 1.调用MySQLRemoteService提供的方法查询首页要显示的数据
        ResultEntity<List<PortalTypeVO>> portalTypeProjectData = sqlRemoteService.getPortalTypeProjectDataRemote();

        // 2.检查查询结果
        String result = portalTypeProjectData.getResult();

        if (ResultEntity.SUCCESS.equals(result)) {

            // 3.获取查询的数据
            List<PortalTypeVO> list = portalTypeProjectData.getData();

            // 4.存入模型
            model.addAttribute(CrowdConstant.ATTR_NAME_PORTAL_DATA, list);
        }

        return "portal";
    }
}
