package com.qiwenshare.common.operation;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class VideoOperation {

    public static InputStream thumbnailsImage(InputStream inputStream, File outFile, int width, int height) throws IOException {

//        File targetFile = new File("E:\\公益广告视频.jpg");
        try {
            FFmpegFrameGrabber ff = new FFmpegFrameGrabber(inputStream);
            ff.start();
            // 视频总帧数
            int videoLength = ff.getLengthInFrames();
            Frame f  = null;
            int i = 0;
            while (i < videoLength) {
                // 过滤前20帧,因为前20帧可能是全黑的
                // 这里看需求，也可以直接根据帧数取图片
                f = ff.grabFrame();
                if (i > 20 && f.image != null) {
                    break;
                }
                i++;
            }
            int owidth = f.imageWidth;
            int oheight = f.imageHeight;
            // 对截取的帧进行等比例缩放
//            int width = 800;
            height = (int) (((double) width / owidth) * oheight);
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage fecthedImage = converter.getBufferedImage(f);
            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            bi.getGraphics().drawImage(fecthedImage.getScaledInstance(width, height, Image.SCALE_SMOOTH),
                    0, 0, null);

            File saveDir = outFile.getParentFile().getAbsoluteFile();
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            ImageIO.write(bi, "jpg", outFile);
            ff.stop();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("AWTError")) {
                log.info(e.getMessage());
            }
            log.error(e.getMessage());
        }
        return new FileInputStream(outFile);


    }
}
