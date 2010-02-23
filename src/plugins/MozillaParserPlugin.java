package plugins;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;

import com.dappit.Dapper.parser.EnviromentController;
import com.dappit.Dapper.parser.MozillaParser;
import com.dappit.Dapper.parser.ParserInitializationException;

import org.mightyfrog.util.xpathtester.Plugin;

import org.dom4j.DocumentException;
import org.w3c.dom.Document;

/**
 *
 * @author Shigehiro Soejima
 */
public class MozillaParserPlugin implements Plugin {
    //
    private Charset charset = Charset.defaultCharset();

    /** */
    @Override
    public Document getDocument(URL url) throws Exception {
        Document doc = null;
        Reader reader = null;
        StringWriter writer = new StringWriter();
        try {
            reader = new InputStreamReader(url.openStream());
            int n = 0;
            char[] c = new char[256];
            while ((n = reader.read(c)) != -1) {
                writer.write(c, 0, n);
            }
            doc = new MozillaParser().parse(writer.toString().getBytes(),
                                            getCharset().toString());
        } catch (IOException e) {
            e.printStackTrace();
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
    public Charset getCharset() {
        return this.charset;
    }

    /** */
    @Override
    public String getName() {
        return "Mozilla Parser Plugin";
    }
}
