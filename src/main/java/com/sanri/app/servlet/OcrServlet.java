//package com.sanri.app.servlet;
//
//import com.sanri.app.BaseServlet;
//import com.sanri.app.ocr.WebApis;
//import com.sanri.app.ocr.YoudaoOcr;
//import com.sanri.frame.RequestMapping;
//import org.apache.commons.fileupload.FileItem;
//import org.apache.commons.io.FilenameUtils;
//import org.apache.commons.lang3.StringUtils;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.List;
//
//@RequestMapping("/ocr")
//public class OcrServlet extends BaseServlet {
//    private YoudaoOcr youdaoOcr = new YoudaoOcr();
//    private WebApis webApis = new WebApis();
//
//    /**
//     * 上传文件识别文本
//     * @param image
//     * @return
//     */
//    public List<String> resolve(FileItem image) throws IOException, IllegalAccessException {
//        InputStream inputStream = image.getInputStream();
//        String name = image.getName();
//        String extension = FilenameUtils.getExtension(name);
//        if(StringUtils.isBlank(extension) || "PNG,JPG,GIF".indexOf(extension.toUpperCase()) == -1){
//            return new ArrayList<String>();
//        }
//        return youdaoOcr.resolve(inputStream);
//    }
//}
