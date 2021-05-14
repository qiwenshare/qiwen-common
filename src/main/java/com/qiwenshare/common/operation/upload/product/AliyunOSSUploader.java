package com.qiwenshare.common.operation.upload.product;

import com.alibaba.fastjson.JSON;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;
import com.qiwenshare.common.config.QiwenFileConfig;
import com.qiwenshare.common.exception.UploadGeneralException;
import com.qiwenshare.common.operation.upload.Uploader;
import com.qiwenshare.common.operation.upload.domain.UploadFile;
import com.qiwenshare.common.util.FileUtil;
import com.qiwenshare.common.util.PathUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
//import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.*;

@Slf4j
@Component
public class AliyunOSSUploader extends Uploader {
    @Resource
    QiwenFileConfig qiwenFileConfig;


    // partETags是PartETag的集合。PartETag由分片的ETag和分片号组成。
    public static Map<String, List<PartETag>> partETagsMap = new HashMap<String, List<PartETag>>();
    public static Map<String, UploadFileInfo> uploadPartRequestMap = new HashMap<>();

    public static Map<String, OSS> ossMap = new HashMap<>();

    @Override
    public List<UploadFile> upload(HttpServletRequest httpServletRequest, UploadFile uploadFile) {
        log.info("开始上传upload");

        List<UploadFile> saveUploadFileList = new ArrayList<>();
        StandardMultipartHttpServletRequest request = (StandardMultipartHttpServletRequest) httpServletRequest;

        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if (!isMultipart) {
            throw new UploadGeneralException("未包含文件上传域");
        }

        Iterator<String> iter = request.getFileNames();
        while (iter.hasNext()) {

            saveUploadFileList = doUpload(request, iter, uploadFile);
        }


        log.info("结束上传");
        return saveUploadFileList;
    }

    private List<UploadFile> doUpload(StandardMultipartHttpServletRequest standardMultipartHttpServletRequest, Iterator<String> iter, UploadFile uploadFile) {
        String savePath = getLocalFileSavePath();
        OSS ossClient = getClient(uploadFile);

        List<UploadFile> saveUploadFileList = new ArrayList<>();

        try {
            MultipartFile multipartfile = standardMultipartHttpServletRequest.getFile(iter.next());

            String timeStampName = getTimeStampName();
            String originalName = multipartfile.getOriginalFilename();
            String fileName = getFileName(originalName);
            String fileType = FileUtil.getFileExtendName(originalName);
            uploadFile.setFileName(fileName);
            uploadFile.setFileType(fileType);
            uploadFile.setTimeStampName(timeStampName);

            String ossFilePath = savePath + FILE_SEPARATOR + timeStampName + FILE_SEPARATOR + fileName + "." + fileType;
            String confFilePath = savePath + FILE_SEPARATOR + uploadFile.getIdentifier() + "." + "conf";
            File confFile = new File(PathUtil.getStaticPath() + FILE_SEPARATOR + confFilePath);

            synchronized (AliyunOSSUploader.class) {
                if (uploadPartRequestMap.get(uploadFile.getIdentifier()) == null) {
                    InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(qiwenFileConfig.getAliyun().getOss().getBucketName(), ossFilePath.substring(1));
                    InitiateMultipartUploadResult upresult = ossClient.initiateMultipartUpload(request);
                    String uploadId = upresult.getUploadId();

                    UploadFileInfo uploadPartRequest = new UploadFileInfo();
                    uploadPartRequest.setBucketName(qiwenFileConfig.getAliyun().getOss().getBucketName());
                    uploadPartRequest.setKey(ossFilePath.substring(1));
                    uploadPartRequest.setUploadId(uploadId);
                    uploadPartRequestMap.put(uploadFile.getIdentifier(), uploadPartRequest);
                }

            }

            UploadFileInfo uploadFileInfo = uploadPartRequestMap.get(uploadFile.getIdentifier());
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(uploadFileInfo.getBucketName());
            uploadPartRequest.setKey(uploadFileInfo.getKey());
            uploadPartRequest.setUploadId(uploadFileInfo.getUploadId());
            uploadPartRequest.setInputStream(multipartfile.getInputStream());
            uploadPartRequest.setPartSize(uploadFile.getCurrentChunkSize());
            uploadPartRequest.setPartNumber(uploadFile.getChunkNumber());
            log.info(JSON.toJSONString(uploadPartRequest));

            UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
            synchronized (AliyunOSSUploader.class) {
                log.info("上传结果：" + JSON.toJSONString(uploadPartResult));
                if (partETagsMap.get(uploadFile.getIdentifier()) == null) {
                    List<PartETag> partETags = new ArrayList<PartETag>();
                    partETags.add(uploadPartResult.getPartETag());
                    partETagsMap.put(uploadFile.getIdentifier(), partETags);
                } else {
                    partETagsMap.get(uploadFile.getIdentifier()).add(uploadPartResult.getPartETag());
                }
            }

            boolean isComplete = checkUploadStatus(uploadFile, confFile);
            if (isComplete) {
                log.info("分片上传完成");
                completeMultipartUpload(uploadFile);

                uploadFile.setUrl("/" + uploadPartRequestMap.get(uploadFile.getIdentifier()).getKey());
                uploadFile.setSuccess(1);
                uploadFile.setMessage("上传成功");
                partETagsMap.remove(uploadFile.getIdentifier());
                uploadPartRequestMap.remove(uploadFile.getIdentifier());
                ossMap.remove(uploadFile.getIdentifier());
            } else {
                uploadFile.setSuccess(0);
                uploadFile.setMessage("未完成");
            }

        } catch (Exception e) {
            log.error("上传出错：" + e);
            throw new UploadGeneralException(e);
        }

        uploadFile.setIsOSS(1);
        uploadFile.setStorageType(1);

        uploadFile.setFileSize(uploadFile.getTotalSize());
        saveUploadFileList.add(uploadFile);
        return saveUploadFileList;
    }

    /**
     * 将文件分块进行升序排序并执行文件上传。
     */
    protected void completeMultipartUpload(UploadFile uploadFile) {

        List<PartETag> partETags = partETagsMap.get(uploadFile.getIdentifier());
        Collections.sort(partETags, Comparator.comparingInt(PartETag::getPartNumber));
        UploadFileInfo uploadFileInfo = uploadPartRequestMap.get(uploadFile.getIdentifier());
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(qiwenFileConfig.getAliyun().getOss().getBucketName(),
                        uploadFileInfo.getKey(),
                        uploadFileInfo.getUploadId(),
                        partETags);
        log.info("----:" + JSON.toJSONString(partETags));
        // 完成上传。
        CompleteMultipartUploadResult completeMultipartUploadResult = getClient(uploadFile).completeMultipartUpload(completeMultipartUploadRequest);
        log.info("----:" + JSON.toJSONString(completeMultipartUploadRequest));
        getClient(uploadFile).shutdown();

//
    }

    private void listFile(UploadFile uploadFile) {
        // 列举已上传的分片，其中uploadId来自于InitiateMultipartUpload返回的结果。
        ListPartsRequest listPartsRequest = new ListPartsRequest(qiwenFileConfig.getAliyun().getOss().getBucketName(), uploadPartRequestMap.get(uploadFile.getIdentifier()).getKey(), uploadPartRequestMap.get(uploadFile.getIdentifier()).getUploadId());
        // 设置uploadId。
        //listPartsRequest.setUploadId(uploadId);
        // 设置分页时每一页中分片数量为100个。默认列举1000个分片。
        listPartsRequest.setMaxParts(100);
        // 指定List的起始位置。只有分片号大于此参数值的分片会被列举。
//            listPartsRequest.setPartNumberMarker(1);
        PartListing partListing = getClient(uploadFile).listParts(listPartsRequest);

        for (PartSummary part : partListing.getParts()) {
            log.info("分片号："+part.getPartNumber() + ", 分片数据大小: "+
                    part.getSize() + "，分片的ETag:"+part.getETag()
                    + "， 分片最后修改时间："+ part.getLastModified());
            // 获取分片号。
            part.getPartNumber();
            // 获取分片数据大小。
            part.getSize();
            // 获取分片的ETag。
            part.getETag();
            // 获取分片的最后修改时间。
            part.getLastModified();
        }

    }

    /**
     * 取消上传
     */
    private void cancelUpload(UploadFile uploadFile) {
        AbortMultipartUploadRequest abortMultipartUploadRequest =
                new AbortMultipartUploadRequest(qiwenFileConfig.getAliyun().getOss().getBucketName(), uploadPartRequestMap.get(uploadFile.getIdentifier()).getKey(), uploadPartRequestMap.get(uploadFile.getIdentifier()).getUploadId());
        getClient(uploadFile).abortMultipartUpload(abortMultipartUploadRequest);
    }

    private synchronized OSS getClient(UploadFile uploadFile) {
        OSS ossClient = null;
        if (ossMap.get(uploadFile.getIdentifier()) == null) {
            ossClient = new OSSClientBuilder().build(qiwenFileConfig.getAliyun().getOss().getEndpoint(), qiwenFileConfig.getAliyun().getOss().getAccessKeyId(), qiwenFileConfig.getAliyun().getOss().getAccessKeySecret());
            ossMap.put(uploadFile.getIdentifier(), ossClient);
        } else {
            ossClient = ossMap.get(uploadFile.getIdentifier());
        }
        return ossClient;
    }

    @Data
    public class UploadFileInfo {
        private String bucketName;
        private String key;
        private String uploadId;
    }

}
