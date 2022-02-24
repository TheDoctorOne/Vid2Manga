package net.mahmutkocas.v2m_conv;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.YoutubeProgressCallback;
import com.github.kiulian.downloader.downloader.request.RequestPlaylistInfo;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.playlist.PlaylistInfo;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.model.videos.formats.VideoFormat;
import com.github.kiulian.downloader.model.videos.quality.VideoQuality;

import javax.naming.MalformedLinkException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.MalformedParametersException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeHandler {

    /**
     * When set to <b>true</b> keeps video files in separate files.
     * <br>
     * If set to <b>false</b>, overwrites the video files under the same name. (vid.'extension')
     * */
    public static boolean KEEP_VIDEO_FILES = false;

    // Thanks for the REGEX : https://stackoverflow.com/a/44955588/7090147
    private static final String YT_URL_TO_ID_REGEX = "http(?:s)?:\\/\\/(?:m.)?(?:www\\.)?youtu(?:\\.be\\/|be\\.com\\/(?:watch\\?(?:feature=youtu.be\\&)?v=|v\\/|embed\\/|user\\/(?:[\\w#]+\\/)+))([^&#?\\n]+)";
    private static final Pattern VIDEO_URL_PATTERN = Pattern.compile(YT_URL_TO_ID_REGEX, Pattern.CASE_INSENSITIVE);

    private static final String YT_PLAYLIST_ID_REGEX = "/(?:\\?v=|\\?list=|be/)(\\w)/g*";
    private static final Pattern PLAYLIST_URL_PATTERN = Pattern.compile(YT_PLAYLIST_ID_REGEX, Pattern.CASE_INSENSITIVE);


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

    public enum ListDownloadOrder {
        ABOVE,
        BELOW
    }


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
    public static void FetchYoutubeListFromIndex(String URL,
                                                 int index,
                                                 ListDownloadOrder order,
                                                 File targetFolder,
                                                 PlayListCallback playListCallback,
                                                 YoutubeCallback<VideoInfo> infoCallback,
                                                 YoutubeProgressCallback<File> fileCallback) throws MalformedLinkException {
        YoutubeDownloader ytDownloader = new YoutubeDownloader();
        RequestPlaylistInfo listInfoReq = new RequestPlaylistInfo(getListId(URL));

        PlaylistInfo info = ytDownloader.getPlaylistInfo(listInfoReq).data();

        if(info == null) {
            throw new MalformedLinkException("Playlist Info is not valid! " + URL);
        }

        List<String> downloadedIds = new ArrayList<>(); //Avoid same video inside the playlist.
        info.videos().forEach(playlistVideoDetails -> {
            boolean shouldDownload = false;

            if(downloadedIds.contains(playlistVideoDetails.videoId()))
                return;

            switch (order) {
                case ABOVE:
                    if (playlistVideoDetails.index() > index) {
                        shouldDownload = true;
                    }
                    break;
                case BELOW:
                    if (playlistVideoDetails.index() < index) {
                        shouldDownload = true;
                    }
                    break;
            }

            if(shouldDownload) {
                if(playListCallback != null) {
                    playListCallback.downloadingIndex(playlistVideoDetails.index());
                }
                boolean res = FetchYoutubeID(playlistVideoDetails.videoId(), targetFolder, infoCallback, fileCallback);

                if(playListCallback != null) {
                    if (res) {
                        playListCallback.downloadedIndex(playlistVideoDetails.index());
                    } else {
                        playListCallback.failedIndex(playlistVideoDetails.index());
                    }
                }

                downloadedIds.add(playlistVideoDetails.videoId()); //Avoid same video inside the playlist.
            }
        });

    }

    public static boolean FetchYoutubeURL(String URL, File targetFolder, YoutubeCallback<VideoInfo> infoCallback, YoutubeProgressCallback<File> fileCallback) {
        return FetchYoutubeID(getVideoId(URL), targetFolder, infoCallback, fileCallback);
    }

    public static boolean FetchYoutubeID(String ID, File targetFolder, YoutubeCallback<VideoInfo> infoCallback, YoutubeProgressCallback<File> fileCallback) {
        YoutubeDownloader ytDownloader = new YoutubeDownloader();
        RequestVideoInfo reqInfo = new RequestVideoInfo(ID)
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
            throw new MalformedParametersException("Format not found! ");
        }

        RequestVideoFileDownload fileDownload = new RequestVideoFileDownload(format)
                .saveTo(targetFolder)
//                    .renameTo(info.details().title())
                .overwriteIfExists(true)
                .callback(fileCallback)
                .async();

        if(!KEEP_VIDEO_FILES) {
            fileDownload.renameTo("vid");

            while(true) {
                try {
                    if (fileDownload.getOutputFile().delete())
                        break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("Can not delete the file: " + fileDownload.getOutputFile().getAbsolutePath());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }

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

    public static String getListId(String listURL) {
        String videoId;
        String listSep = "list=";
        if(listURL.contains(listSep)) {
            videoId = listURL.split(listSep)[1];
        } else {
            videoId = listURL;
        }
        return videoId;
    }


    public interface PlayListCallback {
        default void downloadingIndex(int index) {}
        default void downloadedIndex(int index) {}
        default void failedIndex(int index) {}
    }


}

