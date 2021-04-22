package vip.codehome.fileserver.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
public class FileController {
    @Autowired
    FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity upload(@RequestParam("file") MultipartFile multipartFile) throws IOException {
        String fid=fileService.upload(multipartFile.getInputStream(),multipartFile.getOriginalFilename());
        return ResponseEntity.ok(fid);
    }
    @PostMapping("/remove/{fid}")
    public ResponseEntity remove(@PathVariable("fid")String fid){
        fileService.remove(fid);
        return ResponseEntity.ok("");
    }
    @RequestMapping("/download/{fid}")
    public void download(@PathVariable("fid")String fid, HttpServletResponse response) throws IOException {
      fileService.download(fid,response);
    }
}
