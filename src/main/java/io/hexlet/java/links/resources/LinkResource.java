package io.hexlet.java.links.resources;


import com.mongodb.MongoClient;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Iterator;
import java.util.Random;

@Path("links")
public class LinkResource {
    private static final String URL_KEY = "url";
    private static final String ID_KEY = "id";
    private static final Response ANSWER_404 = Response.status(Response.Status.NOT_FOUND).build();
    private static final MongoCollection<Document> LINKS_COLLECTION;

    static {
        final MongoClient mongo = new MongoClient(); //equal new MongoClient("localhost", 27017);
        final MongoDatabase db = mongo.getDatabase("shortened");
        LINKS_COLLECTION = db.getCollection("links");
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{id}")
    public Response getUrlById(final @PathParam("id") String id) {
        if (id == null || id.isEmpty()) {
            return ANSWER_404;
        }
        final Iterator<Document> resultIterator = LINKS_COLLECTION.find(new Document(ID_KEY, id)).iterator();
        if (!resultIterator.hasNext()) {
            return ANSWER_404;
        }
        final String url = resultIterator.next().getString(URL_KEY);
        if (url == null || "".equals(url)) {
            return ANSWER_404;
        }
        return Response.ok(url).build();
    }

    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response shortUrl(final String url) {
        if (url == null || url.isEmpty()) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).build();
        }
        // 5 attempt to create link, random for first time...
        int attempt = 0;
        while (attempt < 5) {
            final String id = getRandomId();
            final Document newShort = new Document(ID_KEY, id);
            newShort.put(URL_KEY, url);
            try {
                LINKS_COLLECTION.insertOne(newShort);
                return Response.ok(id).build();
            } catch (MongoWriteException e) {
                // just attempt ++
            }
            attempt++;
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    private static String getRandomId() {
        String possibleCharacters = "QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm1234567890";
        StringBuilder idBuilder = new StringBuilder();

        Random rnd = new Random();
        while (idBuilder.length() < 5) {
            int index = (int) (rnd.nextFloat() * possibleCharacters.length());
            idBuilder.append(possibleCharacters.charAt(index));
        }
        return idBuilder.toString();
    }
}