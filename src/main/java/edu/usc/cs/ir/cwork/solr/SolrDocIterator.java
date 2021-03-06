package edu.usc.cs.ir.cwork.solr;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * This iterator iterates over all the pages of solr results.
 * @since 6.0
 */
public class SolrDocIterator implements Iterator<SolrDocument> {

    public static final Logger LOG = LoggerFactory.getLogger(SolrDocIterator.class);
    public static final int DEF_START = 0;
    public static final int DEF_ROWS = 1000;

    private long count = 0;
    private long limit = Long.MAX_VALUE;
    private SolrServer solr;
    private SolrQuery query;
    private long numFound;
    private int nextStart;
    private Iterator<SolrDocument> curPage;
    private SolrDocument next;

    public SolrDocIterator(String solrUrl, String queryStr, int start, int rows,
                           String...fields){
        this(new HttpSolrServer(solrUrl), queryStr, start, rows, null, fields);
    }

    public SolrDocIterator(SolrServer solr, String queryStr, String...fields) {
        this(solr, queryStr, DEF_START, DEF_ROWS, null, fields);
    }

    public SolrDocIterator(SolrServer solr, String queryStr, int start, int rows, String sort,
                           String...fields){
        this.solr = solr;
        this.nextStart = start;
        this.query = new SolrQuery(queryStr);
        this.query.setRows(rows);
        if (fields != null && fields.length > 0) {
            this.query.setFields(fields);
        }
        if (sort != null && !sort.isEmpty()) {
            this.query.set("sort", sort);
        }
        this.next = getNext(true);
        this.count = 1;
    }

    public long getNumFound() {
        return numFound;
    }

    public int getNextStart() {
        return nextStart;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public SolrDocumentList queryNext()  {
        query.setStart(nextStart);
        try {
            LOG.debug("Query {}, Start = {}", query.getQuery(), nextStart);
            QueryResponse response = solr.query(query);
            this.numFound = response.getResults().getNumFound();
            return response.getResults();
        } catch (SolrServerException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public SolrDocument next() {
        SolrDocument tmp = next;
        next = getNext(false);
        count++;
        return tmp;
    }

    private SolrDocument getNext(boolean forceFetch) {
        if (forceFetch || !curPage.hasNext() && nextStart < numFound) {
            //there is more
            SolrDocumentList page = queryNext();
            this.numFound = page.getNumFound();
            this.nextStart += page.size();
            this.curPage = page.iterator();
        }
        return count < limit && curPage.hasNext() ? curPage.next() : null;
    }

}
