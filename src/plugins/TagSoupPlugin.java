package plugins;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.xalan.xsltc.trax.SAX2DOM;

import org.ccil.cowan.tagsoup.Parser;

import org.mightyfrog.util.xpathtester.Plugin;

import org.w3c.dom.Document;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.InputSource;

/**
 *
 * @author Shigehiro Soejima
 */
public class TagSoupPlugin implements Plugin {
    //
    private Charset charset = Charset.defaultCharset();

    /** */
    @Override
    public Document getDocument(URL url) throws Exception {
        Document doc = null;
        Reader reader = null;
        try {
            Parser p = new Parser();
            SAX2DOM sax2dom = new SAX2DOM();
            p.setContentHandler(sax2dom);
            p.setFeature("http://xml.org/sax/features/namespaces", false);
            reader = new InputStreamReader(url.openStream(), getCharset());
            p.parse(new InputSource(reader));
            doc = (Document) sax2dom.getDOM();
        } catch (IOException e) {
            throw e;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }

        return doc;
    }

    /** */
    @Override
    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    /** */
    @Override
    public Charset getCharset() throws UnsupportedOperationException {
        return this.charset;
    }

    /** */
    @Override
    public String getName() {
        return "TagSoup Plugin";
    }
}
