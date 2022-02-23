package net.mahmutkocas.v2m_conv;

import com.github.kiulian.downloader.downloader.YoutubeProgressCallback;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;

public class Main {
    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

//    public static void printHelp() {
//        System.out.println("Usage:\n" +
//                "\t<video-id> <output-dir>");
//    }

    public static void main(String[] args) {


        Mat mat = new Mat(10,10, CvType.CV_32FC1);
        System.out.println("Mat Size : " + mat.rows() + ", " + mat.cols());

        MangaConverter.ExtractFromYoutubeByInterval(args[0],
                new File("test"),
                new File("testImage"), null,
                new YoutubeProgressCallback<File>() {
                    @Override
                    public void onDownloading(int progress) {
                        progress = progress;
                    }

                    @Override
                    public void onFinished(File data) {
                        data = data;
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        try {
                            throw throwable;
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                });
    }
}
