import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import vip.codehome.fileserver.FileServer;
import vip.codehome.fileserver.service.FileService;

/***
 * @author 道士吟诗
 * @date 2021/4/24-下午9:19
 * @description
 ***/
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FileServer.class)
public class GridFsTest {
    @Autowired
    FileService fileService;
    @Test
    public void testChunk(){
        //fileService.download("6082641b3b8c044baecc1fad",0l,100l);
    }
}
