package fb;

import analysis.functions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.VisualRecognition;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.ClassifyImagesOptions;
import com.ibm.watson.developer_cloud.visual_recognition.v3.model.VisualClassification;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import twitter.Element;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;

public class image {
	
	private VisualRecognition service;
	public HashMap<String,Element> imageDb;
	private download download;
	double max = 1;
	
	public image(String token) {
		service = new VisualRecognition(VisualRecognition.VERSION_DATE_2016_05_20);
	    service.setApiKey("e9391b759dfb709442f973a49c65253f17b26b3c");
	    imageDb = new HashMap<String,Element>();
		download = new download(token);
	}
	
	public void processImages(String url) throws IOException, JSONException, ParseException {
		
		String images = null;
		if (url == null) {
			images = download.downloadURL(new String[] {"photos"}, null);
		} else {
			images = download.downloadURL(null, url);
		}
		//System.out.println(images);
		JSONObject fbObj = new JSONObject(images);
		if (url == null) fbObj = fbObj.getJSONObject("photos");
		JSONArray postsObject = fbObj.getJSONArray("data");
		System.out.println(postsObject);
		for (int i=0;i<postsObject.length();i++){
			String id = postsObject.getJSONObject(i).getString("id");
			String datem = postsObject.getJSONObject(i).getString("created_time");
			Date dd = main.date.fbinputFormat.parse(datem);
			JSONArray keywords = run(id);
			//System.out.println(i*100/postsObject.length()+"%");
			for (int j=0;j<keywords.length();j++) {
				if (keywords.getJSONObject(j).has("type_hierarchy")) {
					String tp = keywords.getJSONObject(j).getString("type_hierarchy");
					String[] parts = tp.split("/");
					String finalCategory = parts[parts.length-1];
					if (imageDb.containsKey(finalCategory)) {
						double actuel = imageDb.get(finalCategory).sentiment+1;
						if (actuel>max) {
							max = actuel;
						}
						Element old = imageDb.get(finalCategory);
						old.setSentiment(old.sentiment+1.0);
						imageDb.put(finalCategory, old);
					} else {
						Element entry = new Element(1.0,dd);
						imageDb.put(finalCategory, entry);
					}
				}
			}
		}
		if (fbObj.getJSONObject("paging").has("next")) {
			processImages(fbObj.getJSONObject("paging").getString("next"));
		} else {
			processResults();
		}
		
	}
	
	public void processResults(){
		for (String key : imageDb.keySet()) {
			Element old = imageDb.get(key);
			old.setSentiment(imageDb.get(key).sentiment/max);
			imageDb.put(key,old);
		}
	}
	
	public File getImage(String id) throws IOException {
		File outputFile = new File("output.jpg");
		String urlString = "https://graph.facebook.com/"+id+"/picture?access_token="+ download.fbKEY;
		//System.out.println(urlString);
		URL url = new URL(urlString);
		BufferedImage image = ImageIO.read(url);
		try {
			OutputStream os = new FileOutputStream(outputFile);
	        ImageOutputStream ios = ImageIO.createImageOutputStream(os);
	        ImageIO.write(image, "jpg", ios);
	    } catch (Exception exp) {
	        exp.printStackTrace();
	    }
		return outputFile;
	}
	
	public JSONArray run(String id) throws IOException, JSONException {
		File planet = getImage(id);
	    ClassifyImagesOptions options =
	        new ClassifyImagesOptions.Builder().images(planet).build();
	    VisualClassification result = service.classify(options).execute();
	    JSONArray res = getClasses(result.toString());
	    return res;
	}
	
	public JSONArray getClasses(String data) throws JSONException{
		
		JSONObject imgObj = new JSONObject(data);
		JSONArray classes = imgObj.getJSONArray("images").getJSONObject(0).getJSONArray("classifiers")
				.getJSONObject(0).getJSONArray("classes");
		return classes;
	}
	
	public static void main(String[] args) throws IOException, JSONException, ParseException {
		image im = new image("EAACEdEose0cBAO0g5OWC8FdBTwQ904fpbZBqeEXNpFrjfPyTVKhp8qRWxd7dIuvIVlmRJeaVzFjJKV1q9Pezqb6Eu5CXCSbci8D046ZA1Ta6tqVKKuCTRWbwSS85RvdLUU4R6gxZAkc8nQuILIUyOj0A0KEvB8jtD7SxaoWlgZDZD");
		im.processImages(null);
	}

}