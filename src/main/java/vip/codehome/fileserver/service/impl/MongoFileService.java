package vip.codehome.fileserver.service.impl;

import com.mongodb.BasicDBObject;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.mongodb.client.model.Filters;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import vip.codehome.fileserver.config.FileServerProperties;
import vip.codehome.fileserver.service.FileService;

import javax.print.Doc;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/***
 * @author 道士吟诗
 * @date 2021/4/22-下午10:14
 * @description
 ***/
@Service
@Slf4j
public class MongoFileService implements FileService {
    @Autowired
    MongoProperties properties;
    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private MongoDatabaseFactory mongoDatabaseFactory;

    @Override
    public String upload(InputStream inputStream, String fileName) {
        ObjectId objectId = gridFsTemplate.store(inputStream, fileName);
        return objectId.toHexString();
    }

    @Override
    public void download(String fid, HttpServletResponse response) throws IOException {
        getGridFs().downloadToStream(new ObjectId(fid), response.getOutputStream());
    }

    @Override
    public void remove(String fid) {
        gridFsTemplate.delete(Query.query(Criteria.where("_id").is(fid)));
    }

    /**
     * 读取特定chunk中数据返回
     *
     * @param fid
     * @param start
     * @param end
     * @return
     */
    @Override
    public void downloadChunk(String fid, long start, long end, HttpServletResponse response) throws IOException {
        GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(fid)));
        long fileLength = gridFSFile.getLength();
        int chunkSize = gridFSFile.getChunkSize();
        //开始的chunk,结束的chunk
        long startChunkN = (start / chunkSize);
        if (end == -1) {
            end = (int) fileLength-1;
        }
        System.out.println("start:"+start+",end:"+end);
        int endChunkN = (int) (end / chunkSize);
        byte[] chunckData = new byte[(int) (end - start+1)];
        response.setHeader("Accept-Ranges", "bytes");
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        response.setHeader("Content-Disposition", "attachment;filename=" + gridFSFile.getFilename());
        response.setHeader("Content-Length", String.valueOf(chunckData.length));
        //坑爹地方三：Content-Range，格式为
        // [要下载的开始位置]-[结束位置]/[文件总大小]
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
        BufferedOutputStream outputStream;
        outputStream = new BufferedOutputStream(response.getOutputStream());
        for (long n = startChunkN; n <= endChunkN; n++) {
            Document filter = new Document("files_id", new ObjectId(fid)).append("n", n);//.append("n", new Document("$gte", 0));
            Document document = chunckCollection().find(filter).first();
            org.bson.types.Binary data = (Binary) document.get("data");
            //当前chunck数据
            byte[] tmp = data.getData();
            //当前传输得文件片段
            //计算当前块的起始与结束
            try {
                //都在一块上面
                int tmpStart= (int) (n*chunkSize);
                for(int k=0;k<tmp.length;k++){
                    if((tmpStart+k)>=start&&(tmpStart+k)<=end){
                        outputStream.write(tmp[k]);
                    }
                }
                Thread.sleep(10);
                outputStream.flush();
                response.flushBuffer();
            //    log.info("下载完毕：" + start + "-" + end);
            } catch (ClientAbortException e) {
            //    log.warn("用户停止下载：" + start + "-" + end);
                //捕获此异常表示拥护停止下载
            } catch (IOException e) {
                e.printStackTrace();
             //   log.error("用户下载IO异常，Message：{}", e.getLocalizedMessage());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public List<String> search(String fileName) {
        MongoCursor<GridFSFile> fsFileMongoCursor = gridFsTemplate.find(Query.query(Criteria.where("filename").is(fileName))).iterator();
        while (fsFileMongoCursor.hasNext()) {
            GridFSFile fsFile = fsFileMongoCursor.next();
            System.out.println(fsFile.getFilename());
        }
        return Collections.emptyList();
    }

    @Autowired
    FileServerProperties serverProperties;

    @Override
    public List<String> list(Integer start, Integer size) {
        Query query = new Query().with(Sort.by(Order.asc("uploadDate")));
        List<GridFSFile> gridFSFiles = new ArrayList<>();
        gridFsTemplate.find(query).into(gridFSFiles);
        return gridFSFiles.stream().map(x -> serverProperties.getAddress() + x.getObjectId().toHexString()).collect(Collectors.toList());
    }

    private GridFSBucket getGridFs() {
        return GridFSBuckets.create(mongoDatabaseFactory.getMongoDatabase());
    }

    private MongoCollection<Document> chunckCollection() {
        return mongoDatabaseFactory.getMongoDatabase().getCollection("fs.chunks");
    }

    public static void main(String[] args) {
        System.out.println(41 / 20);
    }
}
