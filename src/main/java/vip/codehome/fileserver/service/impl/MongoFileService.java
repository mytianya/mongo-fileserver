package vip.codehome.fileserver.service.impl;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import vip.codehome.fileserver.config.FileServerProperties;
import vip.codehome.fileserver.service.FileService;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/***
 * @author 道士吟诗
 * @date 2021/4/22-下午10:14
 * @description
 ***/
@Service
public class MongoFileService implements FileService {
    @Autowired
    private GridFsTemplate gridFsTemplate;
    @Autowired
    private MongoDatabaseFactory mongoDatabaseFactory;
    @Override
    public String upload(InputStream inputStream,String fileName) {
        ObjectId objectId=gridFsTemplate.store(inputStream,fileName);
        return objectId.toHexString();
    }

    @Override
    public void download(String fid, HttpServletResponse response) throws IOException {
        GridFSBucket bucket = GridFSBuckets.create(mongoDatabaseFactory.getMongoDatabase());
        bucket.downloadToStream(new ObjectId(fid), response.getOutputStream());
    }

    @Override
    public void remove(String fid) {
        gridFsTemplate.delete(Query.query(Criteria.where("_id").is(fid)));
    }

    @Override
    public List<String> search(String fileName) {
        MongoCursor<GridFSFile> fsFileMongoCursor=gridFsTemplate.find(Query.query(Criteria.where("filename").is(fileName))).iterator();
        while (fsFileMongoCursor.hasNext()){
            GridFSFile fsFile=fsFileMongoCursor.next();
            System.out.println(fsFile.getFilename());
        }
        return Collections.emptyList();
    }
    @Autowired
    FileServerProperties serverProperties;
    @Override
    public List<String> list(Integer start, Integer size) {
        Query query=new Query().with(Sort.by(Order.asc("uploadDate")));
        List<GridFSFile> gridFSFiles=new ArrayList<>();
        gridFsTemplate.find(query).into(gridFSFiles);
        return gridFSFiles.stream().map(x->serverProperties.getAddress()+x.getObjectId().toHexString()).collect(Collectors.toList());
    }

}
