package com.nirvana.service;

import java.util.Objects;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

@Path("/word_filter")
public class SensitiveWordFilterService {

    @GET
    @Path("/{word}")
    @Produces(MediaType.APPLICATION_JSON)
    public FilterResult word_filter(@PathParam("word") String word) {
        if (Objects.isNull(word) || word.trim().isBlank()) {
            return new FilterResult(0, "");
        }
        String filteredWord = SensitiveWordUtil.replaceSensitiveWord(word);
        return new FilterResult(filteredWord.equals(word) ? 0 : 1, filteredWord);
    }

}