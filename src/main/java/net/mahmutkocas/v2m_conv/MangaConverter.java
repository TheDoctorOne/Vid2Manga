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
import org.omg.CORBA.TRANSACTION_MODE;
import org.opencv.core.Mat;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MangaConverter {
    public static double SKIP_START_VIDEO_MS = 12000;
    public static double MANGA_PAGE_INTERVAL_MS = 10000;

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
        MangaConverter.YoutubeHandler.FetchYoutube(URL, vidOutputDir,
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
                            ExtractPhotosByInterval(data, vidInfo.get() != null ? new File(imageOutputDir, vidInfo.get().details().title()) : imageOutputDir, startSkipMs, intervalMs);

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


        Mat frame = new Mat();

        createFolder(outDir);

        while (frameCount < frameLen) {
            if(cap.read(frame)) {
                frameCount++;

                if(!startSkipped) {
                    if (frameCount < startSkipFrameCount) {
                        continue;
                    } else {
                        startSkipped = true;
                    }
                }

                if(intervalCount % intervalFrameCount == 0) {
                    writeToMatToFile(frame, new File(outDir,imageCount + ".jpg"));
                    imageCount++;
                }

                intervalCount++;
            }
        }


    }

    public static void createFolder(File targetFolder) {
        if (!targetFolder.isDirectory()) {
            if (!targetFolder.mkdirs())
                throw new FileSystemNotFoundException("Target folder cannot be created. " + targetFolder.getAbsolutePath());
        }
    }

    private static boolean writeToMatToFile(Mat m, File out) throws IOException {
        if(!out.isFile())
            if(!out.createNewFile())
                throw new FileNotFoundException("Can not create the file. " + out.getAbsolutePath());
        return ImageIO.write(Mat2BufferedImage(m),"jpg", out);
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

    public static class YoutubeHandler {
        // Thanks for the REGEX : https://stackoverflow.com/a/44955588/7090147
        private static String YT_URL_TO_ID_REGEX = "http(?:s)?:\\/\\/(?:m.)?(?:www\\.)?youtu(?:\\.be\\/|be\\.com\\/(?:watch\\?(?:feature=youtu.be\\&)?v=|v\\/|embed\\/|user\\/(?:[\\w#]+\\/)+))([^&#?\\n]+)";
        private static Pattern VIDEO_URL_PATTERN = Pattern.compile(YT_URL_TO_ID_REGEX, Pattern.CASE_INSENSITIVE);

        // Thanks for the formats : https://gist.github.com/sidneys/7095afe4da4ae58694d128b1034e01e2
        private static final int[] PREFERRED_FORMATS_BY_PRIORITY = {85, 84, 83, 82}; // MP4 - 1080 to 360
        private static final VideoQuality[] PREFERRED_QUALITIES_BY_PRIORITY = {
                VideoQuality.highres,
                VideoQuality.hd2880p,
                VideoQuality.hd2160,
                VideoQuality.hd1440,
                VideoQuality.hd1080,
                VideoQuality.hd720,
                VideoQuality.large,
                VideoQuality.medium,
                VideoQuality.small
        };


        private static List<Format> filterByQuality(VideoInfo info, VideoQuality quality) {
            return info.findFormats(element -> {
                if (element instanceof VideoFormat) {
                    VideoFormat f = (VideoFormat) element;
                    if ( (f.extension() == Extension.M4A || f.extension() == Extension.WEBM || f.extension() == Extension.MPEG4) && f.videoQuality() == quality) {
                        return true;
                    }
                }
                return false;
            });
        }

        /**
         * Blocks the thread handle with care.
         */
        public static boolean FetchYoutube(String URL, File targetFolder, YoutubeCallback<VideoInfo> infoCallback, YoutubeProgressCallback<File> fileCallback) {
            YoutubeDownloader ytDownloader = new YoutubeDownloader();
            RequestVideoInfo reqInfo = new RequestVideoInfo(getVideoId(URL))
                    .callback(infoCallback);
            VideoInfo info = ytDownloader.getVideoInfo(reqInfo).data();

            MangaConverter.createFolder(targetFolder);

            if (info == null)
                return false;


            Format format = null;

            for (VideoQuality quality : PREFERRED_QUALITIES_BY_PRIORITY) {
                List<Format> formats = filterByQuality(info, quality);
                if (formats.size() > 0) {
                    format = formats.get(0);
                    break;
                }
            }

            if (format == null) {
                info.videoFormats().forEach(videoFormat -> {
                    System.out.println("Video Format - Ext:" + videoFormat.extension().value() + "\tQuality: " + videoFormat.videoQuality().name());
                });
                throw new RuntimeException("Format not found! ");
            }

            RequestVideoFileDownload fileDownload = new RequestVideoFileDownload(format)
                    .saveTo(targetFolder)
//                    .renameTo(info.details().title())
                    .renameTo("vid")
                    .overwriteIfExists(true)
                    .callback(fileCallback)
                    .async();

            File data = ytDownloader.downloadVideoFile(fileDownload).data();

            System.out.println("File downloaded. " + data.getAbsolutePath());
            return true;
        }

        public static String getVideoId(String videoUrl) {
            String videoId;
            Matcher matcher = VIDEO_URL_PATTERN.matcher(videoUrl);
            if (matcher.find()) {
                videoId = matcher.group(1);
            } else {
                videoId = videoUrl;
            }
            return videoId;
        }

    }

}
