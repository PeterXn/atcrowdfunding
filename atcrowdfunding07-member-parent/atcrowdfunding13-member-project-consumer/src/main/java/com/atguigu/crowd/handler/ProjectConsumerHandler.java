package com.atguigu.crowd.handler;

import com.atguigu.crowd.api.MySQLRemoteService;
import com.atguigu.crowd.config.OSSProperties;
import com.atguigu.crowd.constant.CrowdConstant;
import com.atguigu.crowd.entity.vo.*;
import com.atguigu.crowd.util.CrowdUtil;
import com.atguigu.crowd.util.ResultEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * To change this template use File | Settings | Editor | File and Code Templates.
 *
 * @author Peter
 * @Date 2021/9/12 1:31
 * @description:
 */
@Controller
public class ProjectConsumerHandler {

    @Autowired
    private OSSProperties ossProperties;

    @Autowired
    private MySQLRemoteService sqlRemoteService;

    private final Logger logger = LoggerFactory.getLogger(ProjectConsumerHandler.class);

    @RequestMapping("/get/project/detail/{projectId}")
    public String getProjectDetail(@PathVariable("projectId") Integer projectId, Model model) {

        ResultEntity<DetailProjectVO> resultEntity = sqlRemoteService.getDetailProjectVORemote(projectId);

        if (ResultEntity.SUCCESS.equals(resultEntity.getResult())) {
            DetailProjectVO detailProjectVO = resultEntity.getData();
            model.addAttribute(CrowdConstant.ATTR_NAME_DETAIL_PROJECT_DATA, detailProjectVO);
        }

        return "project-show-detail";
    }


    @RequestMapping("/create/confirm")
    public String saveConfirm(ModelMap modelMap, HttpSession session,
                              MemberConfirmInfoVO memberConfirmInfoVO) {
        // 1.??? Session ?????????????????????????????? ProjectVO ??????
        ProjectVO projectVO = (ProjectVO)
                session.getAttribute(CrowdConstant.ATTR_NAME_TEMPLE_PROJECT);

        // 2.?????? projectVO ??? null
        if (projectVO == null) {
            throw new RuntimeException(CrowdConstant.MESSAGE_TEMPLE_PROJECT_MISSING);
        }

        // 3.?????????????????????????????? projectVO ?????????
        projectVO.setMemberConfirmInfoVO(memberConfirmInfoVO);

        // 4.??? Session ??????????????????????????????
        MemberLoginVO memberLoginVO = (MemberLoginVO)
                session.getAttribute(CrowdConstant.ATTR_NAME_LOGIN_MEMBER);
        Integer memberId = memberLoginVO.getId();

        logger.info("saveConfirm...projectVO: " + projectVO);

        // 5.???????????????????????? projectVO ??????
        ResultEntity<String> saveResultEntity =
                sqlRemoteService.saveProjectVORemote(projectVO, memberId);

        // 6.???????????????????????????????????????
        String result = saveResultEntity.getResult();
        if (ResultEntity.FAILED.equals(result)) {
            modelMap.addAttribute(CrowdConstant.ATTR_NAME_MESSAGE,
                    saveResultEntity.getMessage());

            return "project-confirm";
        }

        // 7.???????????? ProjectVO ????????? Session ?????????
        session.removeAttribute(CrowdConstant.ATTR_NAME_TEMPLE_PROJECT);

        // 8.??????????????????????????????????????????????????????
        return CrowdConstant.REDIRECT_ZUUL + "/project/create/success";
    }


    @ResponseBody
    @RequestMapping("/create/save/return.json")
    public ResultEntity<String> saveReturn(ReturnVO returnVO, HttpSession session) {
        try {
            // 1.???session??????????????????????????? ProjectVO ??????
            ProjectVO projectVO = (ProjectVO)
                    session.getAttribute(CrowdConstant.ATTR_NAME_TEMPLE_PROJECT);

            // 2.??????projectVO?????????null
            if (projectVO == null) {
                return ResultEntity.failed(CrowdConstant.MESSAGE_TEMPLE_PROJECT_MISSING);
            }

            // 3.???projectVO??????????????????????????????????????????
            List<ReturnVO> returnVOList = projectVO.getReturnVOList();

            // 4.?????? returnVOList ??????????????????
            if (returnVOList == null || returnVOList.size() == 0) {
                // 5.????????????????????? returnVOList ???????????????
                returnVOList = new ArrayList<>();

                // 6.???????????????????????????????????????????????? ????????? projectVO ?????????
                projectVO.setReturnVOList(returnVOList);
            }

            // 7.???????????????????????????returnVO??????????????????
            returnVOList.add(returnVO);

            // 8.?????????????????????ProjectVO??????????????????Session?????? ???????????????????????????????????????Redis
            session.setAttribute(CrowdConstant.ATTR_NAME_TEMPLE_PROJECT, projectVO);

            logger.info("saveReturn...projectVO: " + projectVO);

            // 9.????????????????????????????????????
            return ResultEntity.successWithoutData();
        } catch (Exception e) {
            e.printStackTrace();
            return ResultEntity.failed(e.getMessage());
        }
    }


    @ResponseBody
    @RequestMapping("/create/upload/return/picture.json")
    public ResultEntity<String> uploadReturnPicture(
            // ???????????????????????????
            @RequestParam("returnPicture") MultipartFile returnPicture) throws IOException {

        // 1.??????????????????
        ResultEntity<String> uploadReturnPicResultEntity = CrowdUtil.uploadFileToOss(
                ossProperties.getEndPoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret(),
                returnPicture.getInputStream(),
                ossProperties.getBucketName(),
                ossProperties.getBucketDomain(),
                returnPicture.getOriginalFilename());

        // 2.?????????????????????
        logger.info("uploadReturnPicture...uploadReturnPicResultEntity:" + uploadReturnPicResultEntity);

        return uploadReturnPicResultEntity;
    }

    /**
     * @param projectVO
     * @param headerPicture
     * @param detailPictureList
     * @param session
     * @param modelMap
     * @return
     */
    @RequestMapping("/create/project/information")
    public String saveProjectBasicInfo(
            // ???????????????????????????????????????????????????
            ProjectVO projectVO,

            // ?????????????????????
            MultipartFile headerPicture,

            // ???????????????????????????
            List<MultipartFile> detailPictureList,

            // ???????????????????????????????????? ProjectVO ???????????? Session ???
            HttpSession session,

            // ??????????????????????????????????????????????????????????????????????????????
            ModelMap modelMap
    ) throws IOException {

        // ?????? ??????????????????
        // 1.??????????????????
        boolean headerPictureEmpty = headerPicture.isEmpty();

        // ???????????????????????????????????????
        if (headerPictureEmpty) {

            // 2.?????????????????????????????????????????????????????????????????????
            modelMap.addAttribute(CrowdConstant.ATTR_NAME_MESSAGE,
                    CrowdConstant.MESSAGE_HEADER_PIC_EMPTY);

            return "project-launch";
        }

        // 3.???????????????????????????????????????????????????????????????
        ResultEntity<String> uploadHeaderPicResultEntity = CrowdUtil.uploadFileToOss(
                ossProperties.getEndPoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret(),
                headerPicture.getInputStream(),
                ossProperties.getBucketName(),
                ossProperties.getBucketDomain(),
                headerPicture.getOriginalFilename());

        String result = uploadHeaderPicResultEntity.getResult();


        // 4.??????????????????????????????
        if (ResultEntity.FAILED.equals(result)) {
            // ?????????????????????????????????????????????????????????
            modelMap.addAttribute(CrowdConstant.ATTR_NAME_MESSAGE,
                    CrowdConstant.MESSAGE_HEADER_PIC_UPLOAD_FAILED);

            return "project-launch";
        }

        // 5.????????????????????????????????????????????????????????????
        String headerPicturePath = uploadHeaderPicResultEntity.getData();

        logger.info("??????????????????????????????????????????" + headerPicturePath);

        // 6.?????? ProjectVO ?????????
        projectVO.setHeaderPicturePath(headerPicturePath);


        // ?????? ??????????????????
        // 1.???????????????????????????????????????????????????
        List<String> detailPicturePathList = new ArrayList<String>();

        // 2.?????? detailPictureList ????????????
        if (detailPictureList == null || detailPictureList.size() == 0) {
            modelMap.addAttribute(CrowdConstant.ATTR_NAME_MESSAGE,
                    CrowdConstant.MESSAGE_DETAIL_PIC_EMPTY);

            return "project-launch";
        }

        // 3.?????? detailPictureList ??????
        for (MultipartFile detailPicture : detailPictureList) {

            // 4.?????? detailPicture ????????????
            if (detailPicture.isEmpty()) {

                // 5.????????????????????????????????????????????????????????????????????????
                modelMap.addAttribute(CrowdConstant.ATTR_NAME_MESSAGE,
                        CrowdConstant.MESSAGE_DETAIL_PIC_EMPTY);

                return "project-launch";
            }

            // 6.????????????
            ResultEntity<String> detailUploadResultEntity = CrowdUtil.uploadFileToOss(
                    ossProperties.getEndPoint(),
                    ossProperties.getAccessKeyId(),
                    ossProperties.getAccessKeySecret(),
                    detailPicture.getInputStream(),
                    ossProperties.getBucketName(),
                    ossProperties.getBucketDomain(),
                    detailPicture.getOriginalFilename());

            // 7.??????????????????
            String detailUploadResult = detailUploadResultEntity.getResult();

            if (ResultEntity.FAILED.equals(detailUploadResult)) {

                // 9.???????????????????????????????????????????????????????????????
                modelMap.addAttribute(CrowdConstant.ATTR_NAME_MESSAGE,
                        CrowdConstant.MESSAGE_DETAIL_PIC_UPLOAD_FAILED);

                return "project-launch";
            }

            String detailPicturePath = detailUploadResultEntity.getData();

            // 8.??????????????????????????????????????????
            detailPicturePathList.add(detailPicturePath);
        }

        // 10.??????????????????????????????????????????????????? ProjectVO ???
        projectVO.setDetailPicturePathList(detailPicturePathList);

        logger.info("saveProjectBasicInfo...projectVO: " + projectVO);


        // ?????? ????????????
        // 1.??? ProjectVO ???????????? Session ???
        session.setAttribute(CrowdConstant.ATTR_NAME_TEMPLE_PROJECT, projectVO);

        // 2.??????????????????????????????????????????????????????????????????
        return CrowdConstant.REDIRECT_ZUUL + "/project/return/info/page";

    }
}
