package net.mahmutkocas.v2m_conv;

import com.github.kiulian.downloader.downloader.YoutubeProgressCallback;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;

public class Main {
    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public static void main(String[] args) throws IOException, UnsupportedFlavorException {

        MangaConverter.GenerateHTML(new File("C:\\Users\\DoctorOne\\Desktop\\Video2MangaConverter\\testImage\\Strongest Cultivation System Chapter 65 English"));
        String s = (String) Toolkit.getDefaultToolkit()
                .getSystemClipboard().getData(DataFlavor.stringFlavor);

        if(s.trim().equals(""))
            return;

        System.out.println("URL: " + s);
        MangaConverter.ExtractFromYoutubeByInterval(s,
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
