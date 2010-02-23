package org.mightyfrog.util.xpathtester;

import java.net.URL;
import java.nio.charset.Charset;

import org.w3c.dom.Document;

/**
 *
 */
public interface Plugin {
    /**
     * Returns the parse result of the document from the specified url.
     *
     * @param url
     * @throws java.lang.Exception
     */
    public Document getDocument(URL url) throws Exception;

    /**
     *
     * @param charset
     */
    public void setCharset(Charset charset);

    /**
     *
     */
    public Charset getCharset();

    /**
     * Returns the name of this plugin.
     *
     */
    public String getName();
}
