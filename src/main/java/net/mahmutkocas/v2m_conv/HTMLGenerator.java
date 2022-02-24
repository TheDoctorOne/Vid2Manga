package net.mahmutkocas.v2m_conv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HTMLGenerator {

    public static void GenerateHTML(File imageDir, HTMLCallback callback) throws IOException {
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
        sb.append("<div style=\"position:relative; width: 100%; color: white;\">").append("\n");
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
        HTMLGenerator.append(html, sb.toString().getBytes());

        if(callback != null) {
            callback.generationComplete(html);
        }
    }

    /**
     * Appends previous button to current
     * Next button to previous file
     * */
    public static void AppendButtons(File curFile, File prevFile) throws IOException {
        String target_loc = "target_loc";
        String target_but = "target_but";

        String prevButtonValue = "Previous";
        String nextButtonValue = "Next";

        String baseButton = "<form style=\"width: 100%; display: block; margin-left: auto; margin-right: auto;\"" +
                " action=\"" + target_loc + "\">\n" +
                "    <input type=\"submit\" value=\"" + target_but + "\" " +
                "       style=\"width: 100%; display: block; margin-right: auto;\" />\n" +
                "</form>\n";

        String prev = baseButton
                .replaceAll(target_loc, "file:///" + prevFile.getAbsolutePath()
                        .replaceAll("\\\\","/")
                ).replaceAll(target_but,prevButtonValue);

        String next = baseButton
                .replaceAll(target_loc,"file:///" +  curFile.getAbsolutePath()
                        .replaceAll("\\\\","/")
                ).replaceAll(target_but,nextButtonValue);

        HTMLGenerator.append(curFile, prev.getBytes());
        HTMLGenerator.append(prevFile, next.getBytes());
    }


    /**
     * Appends the file if exists. Creates if not exists and writes.
     * */
    private static void append(File file, byte[] bytes) throws IOException {
        FileOutputStream outputStream;
        if(!file.isFile()) {
            outputStream = new FileOutputStream(file);
        } else {
            outputStream = new FileOutputStream(file, true);
        }
        outputStream.write(bytes);
        outputStream.flush();
        outputStream.close();
    }


    public interface HTMLCallback {
        void generationComplete(File html);
    }

}
