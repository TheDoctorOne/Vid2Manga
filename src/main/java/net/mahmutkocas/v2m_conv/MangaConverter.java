package net.mahmutkocas.v2m_conv;

import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.YoutubeProgressCallback;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.naming.MalformedLinkException;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MangaConverter {
    public static double SKIP_START_VIDEO_MS = 12000;
    public static double MANGA_PAGE_INTERVAL_MS = 10000;


    public static void ExtractFromYoutubeByInterval(String URL) {
        ExtractFromYoutubeByInterval(URL, null, null, null);
    }
    public static void ExtractFromYoutubeByInterval(String URL,
                                                    YoutubeCallback<VideoInfo> infoCallback,
                                                    YoutubeProgressCallback<File> fileCallback,
                                                    HTMLGenerator.HTMLCallback onHTMLGenerateComplete) {
        ExtractFromYoutubeByInterval(
                URL,
                new File(new File("").getAbsolutePath()),
                new File(new File("").getAbsolutePath()),
                infoCallback,
                fileCallback,
                onHTMLGenerateComplete);

    }
    public static void ExtractFromYoutubeByInterval(String URL,
                                                    File vidOutputDir,
                                                    File imageOutputDir,
                                                    YoutubeCallback<VideoInfo> infoCallback,
                                                    YoutubeProgressCallback<File> fileCallback,
                                                    HTMLGenerator.HTMLCallback onHTMLGenerateComplete) {

        ExtractFromYoutubeByInterval(URL,
                vidOutputDir,
                imageOutputDir,
                SKIP_START_VIDEO_MS,
                MANGA_PAGE_INTERVAL_MS,
                infoCallback,
                fileCallback,
                onHTMLGenerateComplete);
    }

    public static void ExtractFromYoutubeListByInterval(String URL) throws MalformedLinkException {
        ExtractFromYoutubeListByInterval(URL, 0);
    }

    /**
     * Downloads after the index. Not including the index.
     * */
    public static void ExtractFromYoutubeListByInterval(String URL,
                                                        int index) throws MalformedLinkException {
        ExtractFromYoutubeListByInterval(URL, index, YoutubeHandler.ListDownloadOrder.ABOVE);
    }

    public static void ExtractFromYoutubeListByInterval(String URL,
                                                        int index,
                                                        YoutubeHandler.ListDownloadOrder order) throws MalformedLinkException {
        ExtractFromYoutubeListByInterval(URL, index, order, null, null, null, null);
    }

    public static void ExtractFromYoutubeListByInterval(String URL,
                                                        int index,
                                                        YoutubeHandler.ListDownloadOrder order,
                                                        File imageOutputDir) throws MalformedLinkException {
        ExtractFromYoutubeListByInterval(
                URL,
                index,
                order,
                new File(new File("").getAbsolutePath()),
                null,
                imageOutputDir,
                null,
                null,
                null
        );
    }

    public static void ExtractFromYoutubeListByInterval(String URL,
                                                        int index,
                                                        YoutubeHandler.ListDownloadOrder order,
                                                        File vidOutputDir,
                                                        File imageOutputDir) throws MalformedLinkException {
        ExtractFromYoutubeListByInterval(
                URL,
                index,
                order,
                vidOutputDir,
                null,
                imageOutputDir,
                null,
                null,
                null
        );
    }

    public static void ExtractFromYoutubeListByInterval(String URL,
                                                        int index,
                                                        YoutubeHandler.ListDownloadOrder order,
                                                        YoutubeHandler.PlayListCallback playListCallback,
                                                        YoutubeCallback<VideoInfo> infoCallback,
                                                        YoutubeProgressCallback<File> fileCallback,
                                                        HTMLGenerator.HTMLCallback onHTMLGenerateComplete) throws MalformedLinkException {
        ExtractFromYoutubeListByInterval(
                URL,
                index,
                order,
                new File(new File("").getAbsolutePath()),
                playListCallback,
                new File(new File("").getAbsolutePath()),
                infoCallback,
                fileCallback,
                onHTMLGenerateComplete
        );
    }

    public static void ExtractFromYoutubeListByInterval(String URL,
                                                        int index,
                                                        YoutubeHandler.ListDownloadOrder order,
                                                        File vidOutputDir,
                                                        YoutubeHandler.PlayListCallback playListCallback,
                                                        File imageOutputDir,
                                                        YoutubeCallback<VideoInfo> infoCallback,
                                                        YoutubeProgressCallback<File> fileCallback,
                                                        HTMLGenerator.HTMLCallback onHTMLGenerateComplete) throws MalformedLinkException {
        ExtractFromYoutubeListByInterval(
                URL,
                index,
                order,
                vidOutputDir,
                playListCallback,
                imageOutputDir,
                SKIP_START_VIDEO_MS,
                MANGA_PAGE_INTERVAL_MS,
                infoCallback,
                fileCallback,
                onHTMLGenerateComplete
        );
    }

    public static void ExtractFromYoutubeListByInterval(String URL,
                                                        int index,
                                                        YoutubeHandler.ListDownloadOrder order,
                                                        File vidOutputDir,
                                                        YoutubeHandler.PlayListCallback playListCallback,
                                                        File imageOutputDir,
                                                        double startSkipMs,
                                                        double intervalMs,
                                                        YoutubeCallback<VideoInfo> infoCallback,
                                                        YoutubeProgressCallback<File> fileCallback,
                                                        HTMLGenerator.HTMLCallback onHTMLGenerateComplete
                                                    ) throws MalformedLinkException {

        DownloadCallback callback = DownloadCallback.createDefaultCallback(
                imageOutputDir,
                startSkipMs,
                intervalMs,
                infoCallback, fileCallback, onHTMLGenerateComplete);

        YoutubeHandler.FetchYoutubeListFromIndex(URL,
                index,
                order,
                vidOutputDir,
                playListCallback,
                callback.info,
                callback.downloadCallback
        );
    }

    public static void ExtractFromYoutubeByInterval(String URL,
                                                    File vidOutputDir,
                                                    File imageOutputDir,
                                                    double startSkipMs,
                                                    double intervalMs,
                                                    YoutubeCallback<VideoInfo> infoCallback,
                                                    YoutubeProgressCallback<File> fileCallback,
                                                    HTMLGenerator.HTMLCallback onHTMLGenerateComplete) {
        DownloadCallback callback = DownloadCallback.createDefaultCallback(imageOutputDir, startSkipMs, intervalMs, infoCallback, fileCallback, onHTMLGenerateComplete);
        YoutubeHandler.FetchYoutubeURL(URL, vidOutputDir, callback.info, callback.downloadCallback);
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



    public static class DownloadCallback {

        public static DownloadCallback createDefaultCallback(
                File imageOutputDir,
                double startSkipMs,
                double intervalMs,
                YoutubeCallback<VideoInfo> infoCallback,
                YoutubeProgressCallback<File> fileCallback,
                HTMLGenerator.HTMLCallback onHTMLGenerateComplete) {
            return new DownloadCallback(imageOutputDir, startSkipMs, intervalMs, infoCallback, fileCallback, onHTMLGenerateComplete);
        }

        public AtomicReference<VideoInfo> vidInfo = new AtomicReference<>();
        public List<File> generatedHTMLs = new ArrayList<>();

        public final YoutubeCallback<VideoInfo> info;
        public final YoutubeProgressCallback<File> downloadCallback;
        public final HTMLGenerator.HTMLCallback htmlCallback;


        private DownloadCallback(
                File imageOutputDir,
                double startSkipMs,
                double intervalMs,
                YoutubeCallback<VideoInfo> infoCallback,
                YoutubeProgressCallback<File> fileCallback,
                HTMLGenerator.HTMLCallback onHTMLGenerateComplete) {

            htmlCallback = new HTMLGenerator.HTMLCallback() {
                File prev = null;
                @Override
                public void generationComplete(File html) {
                    generatedHTMLs.add(html);

                    if(prev != null) {
                        try {
                            HTMLGenerator.AppendButtons(html, prev);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if(onHTMLGenerateComplete != null)
                        onHTMLGenerateComplete.generationComplete(html);

                    prev = html;
                }
            };

            info = new YoutubeCallback<VideoInfo>() {
                @Override
                public void onFinished(VideoInfo data) {
                    vidInfo.set(data);
                    if (infoCallback != null)
                        infoCallback.onFinished(data);
                }

                @Override
                public void onError(Throwable throwable) {
                    if (infoCallback != null)
                        infoCallback.onError(throwable);
                }
            };

            downloadCallback = new YoutubeProgressCallback<File>() {
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
                        HTMLGenerator.GenerateHTML(imgOut, DownloadCallback.this.htmlCallback);
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
            };
        }
    }
}
