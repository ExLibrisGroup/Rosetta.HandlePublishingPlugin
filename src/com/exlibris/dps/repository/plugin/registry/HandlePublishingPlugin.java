package com.exlibris.dps.repository.plugin.registry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import org.apache.xmlbeans.XmlException;

import com.exlibris.core.infra.common.exceptions.logging.ExLogger;
import com.exlibris.core.infra.common.util.StringUtils;
import com.exlibris.digitool.common.forms.xmlbeans.XParamsDocument;
import com.exlibris.digitool.repository.api.xmlbeans.Url;
import com.exlibris.dps.sdk.registry.PublisherRegistryPlugin;

public class HandlePublishingPlugin  implements PublisherRegistryPlugin {
	// Template holders
	private static final ExLogger log = ExLogger.getExLogger(HandlePublishingPlugin.class, ExLogger.PUBLISHING);
	private static final String AUTHENTICATION_PUBLIC_INDEX = "adminIndex";
	private static final String AUTHENTICATION_PUBLIC_HANDLENAME = "adminHandleName";
	private static final String AUTHENTICATION_PUBLIC_PUBLICKEYFILE = "publicKeyFile";
	private static final String AUTHENTICATION_PUBLIC_PASSPHRASE = "passphrase";
	private static final String TARGET = "target";
	private static final String PUBLISH = "publish";
	private static final String UNPUBLISH = "unpublish";
	private static final String INDEX = "index";

	Map<String, String> params;

	@Override
	public void initParam(Map<String, String> params) {
		this.params = params;
	}

	@Override
	public boolean publish(String iePid, String convertedIE) {
		XParamsDocument params = null;
		try {
			params = XParamsDocument.Factory.parse(convertedIE);
		} catch (XmlException e) {
			log.error("An error occured while parsing the parameters", e);
			return false;
		}
		// go over each pi and write to batch file
		XParamsDocument.XParams.XParam[] xprms = params.getXParams().getXParamArray();
	    for (int i = 0; i < xprms.length; i++) {
		    XParamsDocument.XParams.XParam param = xprms[i];
		    if(param.getXParamKey()!=null && param.getXParamKey().trim().length()>0) {
		    	if (!publishUnpublishPI(iePid, param.getXParamKey().trim(), param.getXParamValue(), PUBLISH)){
					return false;
				}
		    }
	    }
		return true;
	}

	@Override
	public boolean unpublish(String iePid) {
		return true;
	}

	/**
	 * This methods publish the IE persistence identifier with the handle.net server
	 *
	 * @param control - IE/REP/FILE control
	 * @param pi - Given persistence identifier
	 * @param results
	 * @return
	 */
	private boolean publishUnpublishPI(String pid, String pi,String url, String publishOrUnpublish) {
		// make sure the given PI is valid
		if (StringUtils.isEmptyString(pi)) {
			log.error("Missing Persistence Identifier value.");
			return false;

		}
		PrintWriter fw = null;
		BufferedWriter out = null;
	    try {
			File batchFile = new File(getFileName());
			if(!batchFile.exists()){
				 writeHader(batchFile,fw,out);
			}
			writePiInfo(batchFile,fw,out,pi, url, publishOrUnpublish);
	    }catch ( Exception e ) {
		    log.error("Could not create batchFile!",e);
		    return false;
	    }
	    log.info("Handle operation succeeded.");
	    return true;


	}

	private String getFileName() throws Exception {
		String batchFileName = "batchfile_";
		Calendar cal = Calendar.getInstance();
    	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
    	String dateStr = dateFormat.format(cal.getTime());
		StringBuffer dirPath = new StringBuffer(params.get(TARGET));
		File dir = new File (dirPath.toString());
		if (!dir.exists() || !dir.isDirectory()) {
			throw new Exception("Target directory doesn't exist or is not a directory");
		}
		dirPath.append(File.separator);
		dirPath.append(batchFileName);
		dirPath.append(dateStr);

		return dirPath.toString();
	}

	private void writeHader(File batchFile, PrintWriter fw, BufferedWriter out) throws IOException {

		batchFile.getParentFile().mkdirs();
		batchFile.createNewFile();
		fw  = new PrintWriter(new FileWriter(batchFile, true));
		out = new BufferedWriter(fw );
		out.write("AUTHENTICATE PUBKEY:" + params.get(AUTHENTICATION_PUBLIC_INDEX) +":" + params.get(AUTHENTICATION_PUBLIC_HANDLENAME)+ "\n");
		out.write(params.get(AUTHENTICATION_PUBLIC_PUBLICKEYFILE) +"|" +params.get(AUTHENTICATION_PUBLIC_PASSPHRASE)+ "\n\n");
		out.close();
		fw.close();
	}

	private void writePiInfo(File batchFile, PrintWriter fw, BufferedWriter out,String pi, String url, String publishOrUnpublish) throws IOException {

		fw  = new PrintWriter(new FileWriter(batchFile, true));
		out = new BufferedWriter(fw );
		Url batchUrl = this.getUrl(url);
		out.write(params.get(publishOrUnpublish)+" "+pi +"\n");
		out.write(batchUrl.getType() + " URL 86400 1110 UTF8 " +batchUrl.getStringValue()+ "\n\n");
		out.close();
		fw.close();
	}


	private Url getUrl(String url) {
		Url batchUrl = Url.Factory.newInstance();
		batchUrl.setType(params.get(INDEX));
		batchUrl.setStringValue(url);

		return batchUrl;
	}

}
