package org.OpenGeoportal.Download;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.OpenGeoportal.Download.Methods.PerLayerDownloadMethod;
import org.OpenGeoportal.Download.Types.LayerRequest;
import org.OpenGeoportal.Download.Types.LayerRequest.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;

/**
 * Class to download layers that must be downloaded one at a time.
 * 
 * The actual method for downloading is injected by Spring
 * @author chris
 *
 */
//the layer downloader should handle all the errors thrown by the download method,
//and take care of layer status as much as possible
public class PerLayerDownloader implements LayerDownloader {
	private PerLayerDownloadMethod perLayerDownloadMethod;
	@Autowired
	private DownloadPackager downloadPackager;
	final Logger logger = LoggerFactory.getLogger(this.getClass());

	@SuppressWarnings("unchecked") 
	@Async
	@Override
	public void downloadLayers(UUID requestId, MethodLevelDownloadRequest downloadRequest) throws Exception {
		//downloadStatusManager.addDownloadRequestStatus(requestId, sessionId, layerRequests);
		List<LayerRequest> layerList = downloadRequest.getRequestList();
		for (LayerRequest currentLayer: layerList){
			//this.downloadMethod.validate(currentLayer);
				//check to see if the filename exists
			try {
				logger.info("Requesting download for: " + currentLayer.getLayerNameNS());
				currentLayer.setFutureValue(this.perLayerDownloadMethod.download(currentLayer));
			} catch (Exception e){
				e.printStackTrace();
				logger.error("An error occurred downloading this layer: " + currentLayer.getLayerNameNS());
				currentLayer.setStatus(Status.FAILED);
				continue;
			}
		} 
		int successCount = 0;
		for (LayerRequest currentLayer: layerList){
			try{
				currentLayer.getDownloadedFiles().addAll((Set<File>) currentLayer.getFutureValue().get());
				currentLayer.setStatus(Status.SUCCESS);
				successCount++;
				logger.info("finished download for: " + currentLayer.getLayerNameNS());
			} catch (Exception e){
				logger.error("An error occurred downloading this layer: " + currentLayer.getLayerNameNS());
				currentLayer.setStatus(Status.FAILED);	
			}
		
		}
		
		if (successCount > 0){
			downloadPackager.packageFiles(requestId);
		} else {
			logger.error("No Files to package.  Download failed.");
		}
		
	}

	public PerLayerDownloadMethod getPerLayerDownloadMethod() {
		return perLayerDownloadMethod;
	}

	public void setPerLayerDownloadMethod(PerLayerDownloadMethod perLayerDownloadMethod) {
		this.perLayerDownloadMethod = perLayerDownloadMethod;
	}

	@Override
	public Boolean hasRequiredInfo(LayerRequest layer) {
		return perLayerDownloadMethod.hasRequiredInfo(layer);
	}


}