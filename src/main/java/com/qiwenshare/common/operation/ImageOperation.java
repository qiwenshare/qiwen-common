package com.qiwenshare.common.operation;

import com.qiwenshare.common.util.FileUtil;
import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;

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
     * @param imageSize 图像大小
     * @throws IOException io异常
     */
     public static void thumbnailsImage(File inFile, File outFile, int imageSize) throws IOException {

        Thumbnails.of(inFile).size(imageSize, imageSize)
                .toFile(outFile);

    }

    public static InputStream thumbnailsImage(InputStream inputStream, File outFile, int imageSize) throws IOException {
        File parentFile = outFile.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        String extendName = FileUtil.getFileExtendName(outFile.getPath());
        BufferedImage bufferedImage = Thumbnails.of(inputStream).size(imageSize, imageSize).asBufferedImage();

        Thumbnails.of(bufferedImage).size(imageSize, imageSize).toFile(outFile);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageOutputStream imOut = ImageIO.createImageOutputStream(os);
        ImageIO.write(bufferedImage, extendName, imOut);
        InputStream input = new ByteArrayInputStream(os.toByteArray());
        return input;

    }

}
