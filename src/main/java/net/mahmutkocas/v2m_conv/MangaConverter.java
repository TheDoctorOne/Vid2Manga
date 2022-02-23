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
import org.opencv.video.Video;

import java.io.File;
import java.nio.file.FileSystemNotFoundException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MangaConverter {
    public static double MANGA_PAGE_INTERVAL = 3;

    

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
                    if (f.extension() == Extension.MPEG4 && f.videoQuality() == quality) {
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

            if (!targetFolder.isDirectory()) {
                if (!targetFolder.mkdirs())
                    throw new FileSystemNotFoundException("Target folder cannot be created. " + targetFolder.getAbsolutePath());
            }

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

            if (format == null)
                return false;

            RequestVideoFileDownload fileDownload = new RequestVideoFileDownload(format)
                    .saveTo(targetFolder)
                    .renameTo(info.details().title())
                    .overwriteIfExists(false)
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
