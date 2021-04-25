package vip.codehome.fileserver.rest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vip.codehome.fileserver.config.FileServerProperties;
import vip.codehome.fileserver.service.FileService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/***
 * @author 道士吟诗
 * @date 2021/4/22-下午10:20
 * @description
 ***/
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {
    @Autowired
    FileService fileService;
    @Autowired
    FileServerProperties properties;
    @PostMapping("/upload")
    public ResponseEntity upload(@RequestParam("file") MultipartFile multipartFile) throws IOException {
        String fid=fileService.upload(multipartFile.getInputStream(),multipartFile.getOriginalFilename());
        Map<String,Object> res=new HashMap<String,Object>();
        res.put("fid",fid);
        res.put("fileUrl",properties.getAddress()+fid);
        return ResponseEntity.ok(res);
    }
    @PostMapping("/{fid}/remove")
    public ResponseEntity remove(@PathVariable("fid")String fid){
        fileService.remove(fid);
        return ResponseEntity.ok("");
    }
    @RequestMapping("/{fid}/view")
    public void download(@PathVariable("fid")String fid, HttpServletResponse response) throws IOException {
      fileService.download(fid,response);
    }
    @PostMapping("/list")
    public ResponseEntity list(){
        return ResponseEntity.ok(fileService.list(3,3));
    }
    @RequestMapping("/{fid}/chunkDonwload1")
    public void chunkDownload1(@PathVariable("fid")String fid, HttpServletRequest request,HttpServletResponse response) throws IOException {
        String range = request.getHeader("Range");
        System.out.println(range);
        long startByte = 0;
        long endByte = -1;
        if (range != null && range.contains("bytes=") && range.contains("-")) {
            range = range.substring(range.lastIndexOf("=" )+1).trim();
            String[] ranges = range.split("-");
            if (ranges.length == 1) {
                //range为 bytes=-1111
                if (range.startsWith("-")) {
                    endByte = Long.parseLong(ranges[0]);
                }
                //range为bytes=1111-
                else if (range.endsWith("-")) {
                    startByte = Long.parseLong(ranges[0]);
                }
            } else if (ranges.length == 2) {
                startByte = Long.parseLong(ranges[0]);
                endByte = Long.parseLong(ranges[1]);
            }
        }
        //
        fileService.downloadChunk(fid,startByte,endByte,response);

    }
    @RequestMapping("/{fid}/chunkDonwload")
    public void chunkDownload(@PathVariable("fid")String fid, HttpServletRequest request,HttpServletResponse response) {
        String range = request.getHeader("Range");
        File file = new File("/home/codehome/下载/Conduktor-2.13.1.deb");
        long startByte = 0;
        long endByte = -1;
        if (range != null && range.contains("bytes=") && range.contains("-")) {
            range = range.substring(range.lastIndexOf("=" )+1).trim();
            String[] ranges = range.split("-");
            if (ranges.length == 1) {
                //range为 bytes=-1111
                if (range.startsWith("-")) {
                    endByte = Long.parseLong(ranges[0]);
                }
                //range为bytes=1111-
                else if (range.endsWith("-")) {
                    startByte = Long.parseLong(ranges[0]);
                }
            } else if (ranges.length == 2) {
                startByte = Long.parseLong(ranges[0]);
                endByte = Long.parseLong(ranges[1]);
            }
        }
        //
        byte[] bytes=null;

        long contentLength = endByte - startByte + 1;
        response.setHeader("Accept-Ranges", "bytes");
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        response.setHeader("Content-Disposition", "attachment;filename=" + file.getName());
        response.setHeader("Content-Length", String.valueOf(bytes.length));
        //坑爹地方三：Content-Range，格式为
        // [要下载的开始位置]-[结束位置]/[文件总大小]
        response
            .setHeader("Content-Range", "bytes " + startByte + "-" + endByte + "/" + file.length());
        //当前传输得文件片段

        BufferedOutputStream outputStream;
        RandomAccessFile randomAccessFile = null;
        //已传送数据大小
        long transmitted = 0;
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
            outputStream = new BufferedOutputStream(response.getOutputStream());
            byte[] buff = new byte[4096];
            int len = 0;
            randomAccessFile.seek(startByte);
            //坑爹地方四：判断是否到了最后不足4096（buff的length）个byte这个逻辑（(transmitted + len) <= contentLength）要放前面！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
            //不然会会先读取randomAccessFile，造成后面读取位置出错，找了一天才发现问题所在
            while ((transmitted + len) <= contentLength
                && (len = randomAccessFile.read(buff)) != -1) {
                log.info("current index:{}",len);
                outputStream.write(buff, 0, len);
                transmitted += len;
                //                //停一下，方便测试，用的时候删了就行了
                                Thread.sleep(10);
            }
            //处理不足buff.length部分
            if (transmitted < contentLength) {
                len = randomAccessFile.read(buff, 0, (int) (contentLength - transmitted));
                outputStream.write(buff, 0, len);
                transmitted += len;
            }

            outputStream.flush();
            response.flushBuffer();
            randomAccessFile.close();
            log.info("下载完毕：" + startByte + "-" + endByte + "：" + transmitted);
        } catch (ClientAbortException e) {
            log.warn("用户停止下载：" + startByte + "-" + endByte + "：" + transmitted);
            //捕获此异常表示拥护停止下载
        } catch (IOException e) {
            e.printStackTrace();
            log.error("用户下载IO异常，Message：{}", e.getLocalizedMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
