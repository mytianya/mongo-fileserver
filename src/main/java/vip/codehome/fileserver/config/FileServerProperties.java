package vip.codehome.fileserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author zyw
 * @mail dsyslove@163.com
 * @createtime 2021/4/23--9:08
 * @description
 **/
@ConfigurationProperties("file.server")
@Configuration
@Data
public class FileServerProperties {
  String address="http://localhost:8333/file/view/";
}
