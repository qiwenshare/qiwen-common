package com.qiwenshare.common.operation;

import cn.hutool.core.bean.BeanUtil;
import com.qiwenshare.common.exception.QiwenException;
import com.qiwenshare.common.result.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

@Slf4j
public class ImageOperation {
    /**
     * 左旋
     * @param inFile 源文件
     * @param outFile 目的文件
     * @param angle 角度
     * @throws IOException io异常
     */
    public static void leftTotation(File inFile, File outFile, int angle) throws IOException {
        Thumbnails.of(inFile).scale(1).outputQuality(1).rotate(-angle).toFile(outFile);
    }

    /**
     * 右旋
     * @param inFile 源文件
     * @param outFile 目的文件
     * @param angle 角度
     * @throws IOException io异常
     */
    public static void rightTotation(File inFile, File outFile, int angle) throws IOException {
        Thumbnails.of(inFile).scale(1).outputQuality(1).rotate(angle).toFile(outFile);
    }

    /**
     * 压缩
     * @param inFile 源文件
     * @param outFile 目的文件
     * @param width 图像宽
     * @param height 图像高
     * @throws IOException io异常
     */
     public static void thumbnailsImage(File inFile, File outFile, int width, int height) throws IOException {

        Thumbnails.of(inFile).size(width, height)
                .toFile(outFile);

    }

    public static InputStream thumbnailsImage(InputStream inputStream, File outFile, int width, int height) throws IOException {
        File parentFile = outFile.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        ByteArrayOutputStream baos = cloneInputStream(inputStream);
        // 打开两个新的输入流
        InputStream inputStream1 = new ByteArrayInputStream(baos.toByteArray());
        InputStream inputStream2 = new ByteArrayInputStream(baos.toByteArray());
        BufferedImage bufferedImage = ImageIO.read(inputStream1);
        if (bufferedImage == null) {
            return inputStream2;
        }
        int oriHeight = bufferedImage.getHeight();
        int oriWidth = bufferedImage.getWidth();

        if (oriHeight <= height || oriWidth <= width) {
            ImageIO.write(bufferedImage, FilenameUtils.getExtension(outFile.getName()), outFile);
        } else {
            if (oriHeight < oriWidth) {
                Thumbnails.of(bufferedImage).outputQuality(1).scale(1).sourceRegion(Positions.CENTER, oriHeight, oriHeight).toFile(outFile);
            } else {
                Thumbnails.of(bufferedImage).outputQuality(1).scale(1).sourceRegion(Positions.CENTER, oriWidth, oriWidth).toFile(outFile);
            }
            Thumbnails.of(ImageIO.read(outFile)).outputQuality(0.9).size(width, height).toFile(outFile);

        }
        return new FileInputStream(outFile);

    }

    public static InputStream thumbnailsImageForScale(InputStream inputStream, File outFile, long desFileSize) throws IOException {

        byte[] imageBytes = IOUtils.toByteArray(inputStream);
        if (imageBytes == null || imageBytes.length <= 0 || imageBytes.length < desFileSize * 1024) {
            FileUtils.writeByteArrayToFile(outFile,imageBytes);
            return new FileInputStream(outFile);
        }
        long srcSize = imageBytes.length;
        //double accuracy = getAccuracy(srcSize / 1024);
        double accuracy=0.4;

        while (imageBytes.length > desFileSize * 1024) {
            ByteArrayInputStream is = new ByteArrayInputStream(imageBytes);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(imageBytes.length);
            Thumbnails.of(is)
                    .scale(accuracy)
                    .outputQuality(accuracy)
                    .toOutputStream(outputStream);
            imageBytes = outputStream.toByteArray();
        }

        FileUtils.writeByteArrayToFile(outFile,imageBytes);
        return new FileInputStream(outFile);
    }


//    public static void thumbnailsImageForScale(File inFile, File outFile, long desFileSize) throws IOException {
//
//        Mat mat = opencv_imgcodecs.imread(inFile.getAbsolutePath(), opencv_imgcodecs.IMREAD_REDUCED_COLOR_8);
//        int row = mat.rows();
//        int col = mat.cols();
//
//        int fenmu = col + row;
//
//        double widthRite = (double) col / (double) fenmu;
//        double heightRite = (double) row / (double) fenmu;
//
//        int initSize = 5000;
//        Mat resizedImage = new Mat();
//        int width = (int) (initSize * (widthRite));
//        int height = (int) (initSize * (heightRite));
//
//
//
//        Size size = new Size(width, height);
//        opencv_imgproc.resize(mat, resizedImage, size);
//        File parentFile = outFile.getParentFile();
//        if (!parentFile.exists()) {
//            parentFile.mkdirs();
//        }
//
//        opencv_imgcodecs.imwrite(outFile.getAbsolutePath(),resizedImage);
//
//
//        while (outFile.length() > desFileSize * 1024) {
//            Mat newResizedImage = new Mat();
//            width = (int) ((initSize-=100)  * (widthRite));
//            height = (int) ((initSize-=100) * (heightRite));
//
//
//
//            size = new Size(width, height);
//            opencv_imgproc.resize(mat, newResizedImage, size);
//
//
//            opencv_imgcodecs.imwrite(outFile.getAbsolutePath(),newResizedImage);
//            try {
//
//                newResizedImage.release();
//                newResizedImage.close();
//                newResizedImage =null;
//            } catch (Exception e) {
//                log.error("释放资源失败：2");
//            }
//        }
//
//        try {
//
//            resizedImage.release();
//            resizedImage.close();
//            resizedImage = null;
//
//        } catch (Exception e) {
//            log.error("释放资源失败：1");
//        }
//
//        try {
//
//            mat.release();
//            mat.close();
//            mat = null;
//        } catch (Exception e) {
//            log.error("释放资源失败：2");
//        }
//
//
//
//    }

    public static ImageInfo thumbnailsImageFileToOneK(File oriTempFile) {
        Mat mat = opencv_imgcodecs.imread(oriTempFile.getAbsolutePath(), opencv_imgcodecs.IMREAD_UNCHANGED);
        ImageInfo imageInfo = new ImageInfo();

        try {
            org.apache.commons.imaging.ImageInfo imageInfo1 = Imaging.getImageInfo(oriTempFile);
            BeanUtil.copyProperties(imageInfo1, imageInfo);

        } catch (Exception e) {
           log.error("Imaging.getImageInfo error: {}", e.getMessage());
        }



        int row = mat.rows();
        int col = mat.cols();
        Mat resizedImage = new Mat();
        int resizeWidth = col;
        int resizeHeight = row;

        imageInfo.setImageHeight(resizeHeight);
        imageInfo.setImageWidth(resizeWidth);
        int channels = mat.channels();
        int type = mat.type();
        imageInfo.setChannels(channels);
        imageInfo.setType(type);


        if (resizeWidth > resizeHeight) {



            if ((long) resizeWidth / (long) resizeHeight > 1.83) {
                if (resizeHeight < 1080) {
                    closeMat(mat);
                    return imageInfo;
                }

                resizeWidth = (int) (1080 / ((double) resizeHeight / (double) resizeWidth));
                resizeHeight = 1080;
            } else {
                if (resizeWidth < 1920) {
                    closeMat(mat);
                    return imageInfo;
                }

                resizeHeight = (int) ((double) resizeHeight / (double) resizeWidth * 1920);
                resizeWidth = 1920;
            }
        } else {
            int tmp = resizeHeight;
            resizeHeight = resizeWidth;
            resizeWidth = tmp;

            if ((long) resizeWidth / (long) resizeHeight > 1.83) {
                if (resizeHeight < 1080) {
                    closeMat(mat);
                    return imageInfo;
                }

                resizeWidth = (int) (1080 / ((double) resizeHeight / (double) resizeWidth));
                resizeHeight = 1080;
            } else {
                if (resizeWidth < 1920) {
                    closeMat(mat);
                    return imageInfo;
                }

                resizeHeight = (int) ((double) resizeHeight / (double) resizeWidth * 1920);
                resizeWidth = 1920;
            }

            int tmp1 = resizeHeight;
            resizeHeight = resizeWidth;
            resizeWidth = tmp1;
        }

        Size size = new Size(resizeWidth, resizeHeight);
        try {
            opencv_imgproc.resize(mat, resizedImage, size);
        } finally {
            closeMat(mat);
        }
        try {
            opencv_imgcodecs.imwrite(oriTempFile.getAbsolutePath(), resizedImage);
        } finally {
            closeMat(resizedImage);
        }
        return imageInfo;
    }



    public static void closeMat(Mat mat) {
        try {
            mat.release();

        } catch (Exception e2) {
        }
        try {

            mat.close();
        } catch (Exception e2) {
        }
    }



    /**
     * 获取文件扩展名
     * @param fileName 文件名
     * @return 文件扩展名
     */
    public static String getFileExtendName(String fileName) {
        if (fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }


    private static ByteArrayOutputStream cloneInputStream(InputStream input) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
            return baos;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


}
