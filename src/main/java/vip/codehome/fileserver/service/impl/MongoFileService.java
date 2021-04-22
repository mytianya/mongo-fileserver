package vip.codehome.fileserver.service.impl;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSUploadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import vip.codehome.fileserver.service.FileService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

}
