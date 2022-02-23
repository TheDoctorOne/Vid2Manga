package net.mahmutkocas.v2m_conv;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.YoutubeProgressCallback;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.model.videos.formats.VideoFormat;
import com.github.kiulian.downloader.model.videos.quality.VideoQuality;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MangaConverter {
    public static double SKIP_START_VIDEO_MS = 12000;
    public static double MANGA_PAGE_INTERVAL_MS = 10000;

    public static void GenerateHTML(File imageDir) throws IOException {
        /*
        * <h1 style="text-align: center;">name</h1>
        * <div style="width: 100%;">
        * <img style="display: block;" src="<image-path>" alt="" />
        * </div>
        * */
        if(!imageDir.exists())
            return;

        String folderName = imageDir.getName();

        StringBuilder sb = new StringBuilder();

        sb.append("<body style=\" background:black; width: 100%; height: 100%;\">").append("\n");
        sb.append("<div style=\"position:relative; width: 100%; height: 100%; color: white;\">").append("\n");
        sb.append("<h1 style=\"text-align: center;\">").append(folderName).append("</h1>").append("\n");



        File[] imageList = imageDir.listFiles((dir, name) -> name.endsWith(".jpg"));
        List<String> images = new ArrayList<>();
        for(File img : imageList) {
            images.add(img.getName());
        }

        images.sort((o1, o2) -> {
            int i1 = Integer.parseInt(o1.replaceAll(".jpg", ""));
            int i2 = Integer.parseInt(o2.replaceAll(".jpg", ""));

            return Integer.compare(i1, i2);
        });


        for(String img : images) {
            //<img style="display: block;" src="<image-path>" alt="" />
            sb.append("<img style=\"display: block; margin-left: auto; margin-right: auto;\" src=\"").append(img).append("\"/>").append("\n");
        }

        sb.append("</div>");
        sb.append("</body>");


        File html = new File(imageDir,folderName + ".html");
        FileOutputStream outputStream = new FileOutputStream(html);
        outputStream.write(sb.toString().getBytes());
        outputStream.flush();
        outputStream.close();
    }

    public static void ExtractFromYoutubeByInterval(String URL,
                                                    File vidOutputDir,
                                                    File imageOutputDir,
                                                    YoutubeCallback<VideoInfo> infoCallback,
                                                    YoutubeProgressCallback<File> fileCallback) {

        ExtractFromYoutubeByInterval(URL,
                vidOutputDir,
                imageOutputDir,
                SKIP_START_VIDEO_MS,
                MANGA_PAGE_INTERVAL_MS,
                infoCallback,
                fileCallback);
    }
    public static void ExtractFromYoutubeByInterval(String URL,
                                                    File vidOutputDir,
                                                    File imageOutputDir,
                                                    double startSkipMs,
                                                    double intervalMs,
                                                    YoutubeCallback<VideoInfo> infoCallback,
                                                    YoutubeProgressCallback<File> fileCallback) {
        AtomicReference<VideoInfo> vidInfo = new AtomicReference<>();
        YoutubeHandler.FetchYoutube(URL, vidOutputDir,
                new YoutubeCallback<VideoInfo>() {
                    @Override
                    public void onFinished(VideoInfo data) {
                        vidInfo.set(data);
                        if(infoCallback != null)
                            infoCallback.onFinished(data);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        if(infoCallback != null)
                            infoCallback.onError(throwable);
                    }
                }
                ,
                new YoutubeProgressCallback<File>() {
                    @Override
                    public void onDownloading(int progress) {
                        if (fileCallback != null)
                            fileCallback.onDownloading(progress);
                    }

                    @Override
                    public void onFinished(File data) {
                        try {
                            System.out.println("Download Finished. " + data.getAbsolutePath());
                            File imgOut = vidInfo.get() != null ? new File(imageOutputDir, vidInfo.get().details().title()) : imageOutputDir;
                            String absPath = imgOut.getAbsolutePath();
                            int c = 1;
                            while (imgOut.isDirectory())
                                imgOut = new File(absPath + " (" + (c++) + ")");
                            ExtractPhotosByInterval(data, imgOut, startSkipMs, intervalMs);
                            System.out.println("Photos extracted. " + imgOut.getAbsolutePath());
                            GenerateHTML(imgOut);
                            System.out.println("HTML Generated.");

                            if (fileCallback != null)
                                fileCallback.onFinished(data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        if (fileCallback != null)
                            fileCallback.onError(throwable);
                    }
                });
    }

    public static void ExtractPhotosByInterval(File inputVid, File outDir) throws IOException {
        ExtractPhotosByInterval(inputVid, outDir, SKIP_START_VIDEO_MS, MANGA_PAGE_INTERVAL_MS);
    }

    public static void ExtractPhotosByInterval(File inputVid, File outDir, double skipStartMs, double intervalMs) throws IOException {
        VideoCapture cap = new VideoCapture(inputVid.getAbsolutePath());

        if(!cap.isOpened()) {
            throw new IOException("Can't open the video file: " + inputVid.getAbsolutePath());
        }

        double fps = cap.get(Videoio.CAP_PROP_FPS);
        double width = cap.get(Videoio.CAP_PROP_FRAME_WIDTH);
        double height = cap.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        double frameLen = cap.get(Videoio.CAP_PROP_FRAME_COUNT);

        boolean startSkipped = false;
        long startSkipFrameCount = (long) ( fps * (skipStartMs/1000) );
        long intervalFrameCount  = (long) ( fps * (intervalMs/1000) );
        long frameCount = 0;
        long intervalCount = 0;
        long imageCount = 1;
        long timeoutCount = 0;
        long timeoutLimit = 100;


        Mat frame = new Mat();

        createFolder(outDir);

        while (frameCount < frameLen) {
            if(cap.read(frame)) {
                timeoutCount = 0;
                frameCount++;

                if(!startSkipped) {
                    if (frameCount < startSkipFrameCount) {
                        continue;
                    } else {
                        startSkipped = true;
                    }
                }

                if(intervalCount % intervalFrameCount == 0) {
                    writeMatToFile(frame, new File(outDir,imageCount + ".jpg"));
                    imageCount++;
                }

                intervalCount++;
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                timeoutCount++;
                if(timeoutCount > timeoutLimit)
                    break;
            }
        }


    }

    public static void createFolder(File targetFolder) {
        if (!targetFolder.isDirectory()) {
            if (!targetFolder.mkdirs())
                throw new FileSystemNotFoundException("Target folder cannot be created. " + targetFolder.getAbsolutePath());
        }
    }

    private static boolean writeMatToFile(Mat m, File out) throws IOException {
        if(!out.isFile())
            if(!out.createNewFile())
                throw new FileNotFoundException("Can not create the file. " + out.getAbsolutePath());
        return Imgcodecs.imwrite(out.getAbsolutePath(), m);
//        return ImageIO.write(Mat2BufferedImage(m),"jpg", out);
    }

    public static BufferedImage Mat2BufferedImage(Mat m) {
        //Method converts a Mat to a Buffered Image
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if ( m.channels() > 1 ) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        int bufferSize = m.channels()*m.cols()*m.rows();
        byte [] b = new byte[bufferSize];
        m.get(0,0,b); // get all the pixels
        BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);
        return image;
    }
}
