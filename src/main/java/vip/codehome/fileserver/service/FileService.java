package vip.codehome.fileserver.service;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/***
 * @author 道士吟诗
 * @date 2021/4/22-下午10:13
 * @description
 ***/
public interface FileService {
    String upload(InputStream inputStream,String fileName);
    void download(String fid, HttpServletResponse response) throws IOException;
    void remove(String fid);
    List<String> search(String fileName);
    List<String> list(Integer start,Integer size);
}
