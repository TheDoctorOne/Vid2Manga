package net.mahmutkocas.v2m_conv;

import com.github.kiulian.downloader.downloader.YoutubeProgressCallback;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import javax.naming.MalformedLinkException;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;

public class Main {
    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public static void main(String[] args) throws IOException, UnsupportedFlavorException, MalformedLinkException {

        MangaConverter.GenerateHTML(new File("C:\\Users\\DoctorOne\\Desktop\\Video2MangaConverter\\testImage\\Strongest Cultivation System Chapter 65 English"));
        String s = (String) Toolkit.getDefaultToolkit()
                .getSystemClipboard().getData(DataFlavor.stringFlavor);

        if(s.trim().equals(""))
            return;

        System.out.println("URL: " + s);

        MangaConverter.ExtractFromYoutubeListByInterval(s, 65, YoutubeHandler.ListDownloadOrder.ABOVE);
    }
}
