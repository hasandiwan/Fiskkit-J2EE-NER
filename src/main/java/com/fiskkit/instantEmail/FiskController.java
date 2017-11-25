package com.fiskkit.instantEmail;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chargebee.Environment;
import com.chargebee.models.Subscription;
import com.fiskkit.instantEmail.models.FacebookPermissions;
import com.fiskkit.instantEmail.models.Seen;
import com.fiskkit.instantEmail.models.User;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Request.Builder;
import com.squareup.okhttp.Response;

import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import net.sourceforge.schemaspy.Main;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

@RestController
@Component
public class FiskController {
	private static final Logger logger = LoggerFactory.getLogger(FiskController.class);
	private static OkHttpClient client = new OkHttpClient();

	public static final String SENTENCE_LOCATION_KEY = "com.fiskkit.instantEmail.SentenceTokenizer";

	@Value(" fiskkit.diffbotKey")
	public static String DIFFBOT_KEY;
	private static File binFile;

	@Autowired
	UserRepository repository;

	@Autowired
	SeenRepository seenRepository;

	@Value("${chargebee.applicationEnvironment}")
	String chargebeeEnvironment;

	@Value("${chargebee.applicationSecret}")
	String chargebeeSecret;

	@Value("${fiskkit.tweetMessage}")
	String TWITTER_MESSAGE;

	@RequestMapping(value = { "/v1/adjectives", "/adjectives" }, method = RequestMethod.POST)
	public ResponseEntity<Map<String, List<String>>> posCount(@RequestBody String text) {
		Map<String, List<String>> partsOfSpeech = new HashMap<>();
		Request request = new Builder().url(
				"https://github.com/moritzfl/jtopia-configurator/blob/master/src/main/resources/de/moritzf/jtopia/configurator/stanford-wsj-0-18-caseless-left3words-distsim.tagger?raw=true")
				.build();
		Response response;
		byte[] bais = null;
		try {
			response = client.newCall(request).execute();
			bais = response.body().bytes();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		File temp = null;
		if (bais != null) {
			try {
				temp = File.createTempFile("wsj-0-18-caseless-left3words-distsim", ".tagger");
				FileOutputStream fos = new FileOutputStream(temp);
				fos.write(bais);
				fos.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		HashSet<String> posTags = new HashSet<>();
		Collections.addAll(posTags, "CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNP",
				"NNS", "NNPS", "PDT", "POS", "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO", "UH", "VB", "VBD",
				"VBG", "VBN", "VBP", "VBZ", "WDT", "WP", "WP$", "WRB");
		for (String s : posTags) {
			partsOfSpeech.put(s, new ArrayList<String>());
		}
		
		MaxentTagger tagger = new MaxentTagger(temp.getAbsolutePath());
		String taggedString = tagger.tagString(text);
		throw new RuntimeException(taggedString + " <=== taggedString!");
	/*	while (tokenizer.hasNext()) {
			Word w = tokenizer.next();
			int typeInt = w.hashCode();
			// TODO find an enum for the Penn Treebank word class and then add it to the Map
			
			partsOfSpeech.get(type).add(w.word());
		} */

		//return new ResponseEntity<>(partsOfSpeech, HttpStatus.ACCEPTED);
	}

	@RequestMapping(value = { "/v1/tweet/{article}", "/tweet/{article}" }, method = RequestMethod.GET)
	public ResponseEntity<String> tweet(@PathVariable String article, @RequestParam(name = "title") String title) {
		RateLimiter rateLimiter = RateLimiter.create(10);
		rateLimiter.acquire();

		Twitter twitter = new TwitterFactory().getInstance();
		try {
			// get request token.
			// this will throw IllegalStateException if access token is already
			// available
			RequestToken requestToken = twitter.getOAuthRequestToken(System.getProperty("oauth.accessToken"));
			logger.debug("Request token: " + requestToken.getToken());
			logger.debug("Request token secret: " + requestToken.getTokenSecret());

			AccessToken accessToken = twitter.getOAuthAccessToken();

			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			while (null == accessToken) {
				logger.debug("Open the following URL and grant access to your account:");
				logger.debug(requestToken.getAuthorizationURL());
				System.out.print("Enter the PIN(if available) and hit enter after you granted access.[PIN]:");
				String pin = br.readLine();
				try {
					if (pin.length() > 0) {
						accessToken = twitter.getOAuthAccessToken(requestToken, pin);
					} else {
						accessToken = twitter.getOAuthAccessToken(requestToken);
					}
				} catch (TwitterException te) {
					if (401 == te.getStatusCode()) {
						logger.error("Unable to get the access token.", te);
					} else {
					}
				}
			}
			logger.debug("Access token: " + accessToken.getToken());
			logger.debug("Access token secret: " + accessToken.getTokenSecret());
		} catch (IllegalStateException ie) {
			// access token is already available, or consumer key/secret is not
			// set.
			if (!twitter.getAuthorization().isEnabled()) {
				logger.error("OAuth consumer key/secret is not set.", ie);
				return new ResponseEntity<>(
						"Oauth authentication error, make sure your key/secret are correct in twitter4j.properties",
						HttpStatus.UNAUTHORIZED);
			}
		} catch (TwitterException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		try {
			twitter.getOAuthRequestToken();
		} catch (TwitterException e1) {
			logger.error(e1.getMessage(), e1);
		} catch (IllegalStateException e) {
		}
		if (!twitter.getAuthorization().isEnabled()) {
			logger.warn("OAuth consumer key/secret is not set.");
			return new ResponseEntity<>("", HttpStatus.UNAUTHORIZED);
		}
		String source = null;
		try {
			Connection conn = DriverManager.getConnection(
					"jdbc:mysql://aa106w2ihlwnfld.cwblf8lajcuh.us-west-1.rds.amazonaws.com/ebdb?user=root&password=Dylp-Oid-yUl-e&ssl=true");
			PreparedStatement prepped = conn.prepareStatement(
					"select a.author_twitter,a.title,f.created_at,article_id,a.id from fisks f join articles a on article_id = a.id where a.title = ?");
			prepped.setString(1, title);
			logger.info("About to execute " + prepped.toString());
			ResultSet articleMapping = prepped.executeQuery();
			articleMapping.next();
			source = articleMapping.getString("author_twitter");
		} catch (SQLException e1) {
			logger.warn(e1.getMessage(), e1);
		}

		if (source == null) {
			source = "hdiwan";
		}
		SecureRandom sRandom = new SecureRandom();
		byte[] randomBytes = new byte[4];
		sRandom.nextBytes(randomBytes);
		String randomString = randomBytes.toString().replaceAll("@", "");
		logger.info(randomString);
		String message = TWITTER_MESSAGE.replace("$twitterScreenname", "@" + source)
				.replace("$link", String.format("http://fiskkit.com/articles/%s/fisk/discuss", article))
				.replace("$random", randomString);
		logger.info("About to tweet " + message);
		Status status = null;
		try {
			status = twitter.updateStatus(message);
		} catch (TwitterException e) {
			logger.warn(e.getMessage(), e);
		}

		return new ResponseEntity<String>(status.getText(), HttpStatus.OK);
	}

	@RequestMapping(value = { "/facebook", "/v1/facebook" }, method = RequestMethod.GET)
	public ResponseEntity<Boolean> facebook(@RequestParam(name = "title") String article,
			@RequestParam(name = "email") String email) {
		RateLimiter rateLimiter = RateLimiter.create(10);
		rateLimiter.acquire();

		String fbToken = null;

		EntityManager em = Persistence.createEntityManagerFactory("FacebookPermissions").createEntityManager();
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<FacebookPermissions> permitted = cb.createQuery(FacebookPermissions.class);
		TypedQuery<FacebookPermissions> query = em.createQuery(permitted);
		List<FacebookPermissions> allPermissions = query.getResultList();

		for (FacebookPermissions permission : allPermissions) {
			if (permission.getEmail().toLowerCase().equals(email)) {
				if (permission.getPermission().toLowerCase().equals("publish_stream")) {
					fbToken = permission.getToken();
				}
			}
		}

		if (fbToken == null) {
			return new ResponseEntity<Boolean>(Boolean.FALSE, HttpStatus.PRECONDITION_FAILED);
		}

		Facebook facebook = new FacebookFactory().getInstance(new facebook4j.auth.AccessToken(fbToken));
		String message = TWITTER_MESSAGE.replace("$twitterScreenname", "")
				.replace("$link", String.format("http://fiskkit.com/articles/%s/fisk/discuss", article))
				.replace("$random", "1");
		try {
			facebook.postStatusMessage(message);
		} catch (FacebookException e) {
			logger.error(e.getClass().getName() + " caught, stacktrace to follow");
			logger.info(e.getMessage(), e);
			return new ResponseEntity<Boolean>(Boolean.FALSE, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		return new ResponseEntity<Boolean>(Boolean.TRUE, HttpStatus.CREATED);
	}

	@RequestMapping(value = { "/valid", "/v1/valid" }, method = RequestMethod.GET)
	public ResponseEntity<Boolean> getBalance(@RequestParam(name = "subscription") String subscriptionId) {
		Environment.configure(chargebeeEnvironment, chargebeeSecret);
		logger.info("susbscription id requested: " + subscriptionId);
		try {
			Subscription.retrieve(subscriptionId).request().subscription().status();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			return new ResponseEntity<>(Boolean.FALSE, HttpStatus.FAILED_DEPENDENCY);
		}
		return new ResponseEntity<>(Boolean.TRUE, HttpStatus.OK);
	}

	@RequestMapping(value = { "/v1/preview", "/preview" }, method = RequestMethod.GET)
	public ResponseEntity<Map<String, String>> getPreview(@RequestParam(name = "loc") String loc) {
		final Map<String, String> response = new HashMap<>();
		URI ourUrl = null;
		try {
			ourUrl = new URI(loc);
		} catch (URISyntaxException e1) {
			logger.error(e1.getMessage(), e1);
		}

		HttpUrl.Builder urlBuilder = new HttpUrl.Builder();
		urlBuilder.scheme(ourUrl.getScheme()).host(ourUrl.getHost()).addPathSegment("favicon.ico");
		Request request = new Request.Builder().url(urlBuilder.build().toString()).build();
		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Request call, IOException e) {
				e.printStackTrace();
			}

			@Override
			public void onResponse(final Response resp) throws IOException {
				if (!resp.isSuccessful()) {
					throw new IOException("Unexpected code " + response);
				} else {
					response.put("image", Base64.encodeBase64String(resp.body().bytes()));
					response.put("url", loc);
					response.put("timestamp", String.format("%ld", System.currentTimeMillis()));
				}
			}
		});

		urlBuilder = new HttpUrl.Builder();
		urlBuilder.scheme("http").host("api.smmry.com");
		urlBuilder.addQueryParameter("SM_API_KEY", "DEBCF9575D");
		urlBuilder.addQueryParameter("SM_QUOTE_AVOID", "true");
		urlBuilder.addQueryParameter("SM_URL", loc);
		request = null;
		request = new Request.Builder().url(urlBuilder.build().toString()).build();
		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Request call, IOException e) {
				e.printStackTrace();
			}

			@SuppressWarnings("unchecked")
			@Override
			public void onResponse(final Response resp) throws IOException {
				if (resp.isSuccessful()) {
					logger.info(resp.body().string());
					Map<String, String> myMap = null;
					try {
						myMap = new Gson().fromJson(resp.body().string(), Map.class);
						String pageSummary = myMap.get("sm_api_content");
						response.put("summary", pageSummary);
					} catch (Throwable t) {
						throw new IOException(resp.body().string());
					}
				} else {
					throw new RuntimeException("smrry.com failed");
				}
			}
		});

		while ((!response.containsKey("image")) || (!response.containsKey("summmary"))) {
			// wait for remoe APIs to respond
		}

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@RequestMapping(value = { "/v1", "/" }, method = RequestMethod.POST)
	public ResponseEntity<String> newOrg(@RequestParam(name = "id") String organizationUniqueId,
			@RequestParam(name = "subscription") String subscriptionId) {
		User user = new User();
		user.setPhpId(Integer.parseInt(organizationUniqueId));
		user.setChargebeeId(subscriptionId);
		repository.save(user);
		return new ResponseEntity<String>(user.toString(), HttpStatus.CREATED);
	}

	@RequestMapping(value = { "/analyze", "/v1/analyze" }, method = RequestMethod.POST)
	public ResponseEntity<Map<String, String>> statistics(@RequestBody String text) {
		RateLimiter rateLimiter = RateLimiter.create(10);
		rateLimiter.acquire();

		try {
			text = URLDecoder.decode(text, StandardCharsets.UTF_8.toString()).toLowerCase();
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);
		}
		Tokenizer<Word> ptbt = PTBTokenizer.factory().getTokenizer(new StringReader(text));
		Map<String, String> ret = new HashMap<>();
		List<Word> words = ptbt.tokenize();

		Integer wordCount = words.size();
		ret.put("wordCount", wordCount.toString());
		HashMultiset<Word> frequencies = HashMultiset.create();

		Double totalLength = 0.0;
		for (Word word : words) {
			totalLength += word.word().length();
			frequencies.add(word);
		}
		ret.put("averageWordLength", String.format("%f", totalLength / wordCount));

		Double commonCount = Math.floor(words.size() % 10);

		logger.info("threshold for commonality " + commonCount.intValue());

		Set<Word> entries = frequencies.elementSet();

		String freqs = Joiner.on(",").join(", ",
				entries.stream().filter(p -> frequencies.count(p) > commonCount).collect(Collectors.toSet()));
		freqs = freqs.replace("[", "").replace("]", "");
		ret.put("mostCommonWords", freqs);
		logger.info("returning " + new Gson().toJson(ret));
		return new ResponseEntity<Map<String, String>>(ret, HttpStatus.OK);
	}

	@RequestMapping(value = { "/callback", "/v1/callback" }, method = RequestMethod.POST)
	public ResponseEntity<String> chargebeeWebhooks(@RequestParam Map<String, String> params,
			@RequestBody String rawBody) {
		JSONObject json = null;
		try {
			json = new JSONObject(rawBody);
		} catch (JSONException e) {
			logger.error(e.getMessage(), e);
		}
		String customerId = null;
		try {
			customerId = json.getJSONObject("content").getJSONObject("customer").getString("id");
		} catch (JSONException e) {
			logger.error(e.getMessage(), e);
		}
		String customerFirstName = null;
		try {
			customerFirstName = json.getJSONObject("content").getJSONObject("customer").getString("first_name");
		} catch (JSONException e) {
			logger.error(e.getMessage(), e);
		}
		String customerLastName = null;
		try {
			customerLastName = json.getJSONObject("content").getJSONObject("customer").getString("last_name");
		} catch (JSONException e) {
			logger.error(e.getMessage(), e);
		}
		User user = new User();
		user.setChargebeeId(customerId);
		JSONObject remoteJson;
		try {
			remoteJson = new JSONObject(
					(String) new URL("http://fiskkit-dev-2014-11.elasticbeanstalk.com/api/v1/users/").openConnection()
							.getContent());
			JSONArray users = remoteJson.getJSONArray("users");
			for (int i = 0; i != users.length(); i++) {
				JSONObject aUser = users.getJSONObject(i);
				if (aUser.getString("first_name").equals(customerFirstName)
						&& aUser.getString("last_name").equals(customerLastName)) {
					user.setPhpId(Integer.parseInt(aUser.getString("id")));
					repository.save(user);
					return new ResponseEntity<String>("successful", HttpStatus.CREATED);
				}
			}
		} catch (JSONException | IOException e) {
			logger.error(e.getMessage(), e);
		}
		return new ResponseEntity<String>("failed", HttpStatus.CONFLICT);
	}

	@RequestMapping(value = { "/v1/url", "/url" }, method = RequestMethod.GET)
	public ResponseEntity<Boolean> isUrl(@RequestParam(name = "url") String loc) {
		try {
			new URL(loc);
		} catch (MalformedURLException e) {
			return new ResponseEntity<Boolean>(Boolean.FALSE, HttpStatus.OK);
		}
		return new ResponseEntity<Boolean>(Boolean.TRUE, HttpStatus.OK);
	}

	@RequestMapping(value = { "/v1/readability", "/readability" }, method = RequestMethod.POST)
	public ResponseEntity<Double> readability(@RequestBody String text) {
		RateLimiter rateLimiter = RateLimiter.create(5);
		rateLimiter.acquire();

		Double ADJUSTMENT = 3.6365, score = 0.0, DIFFICULT_WORD_THRESHOLD = 0.05;
		String[] wordsInText = text.split("[\\W]");
		HashSet<String> words = (HashSet<String>) Arrays.stream(wordsInText).collect(Collectors.toSet());
		HashSet<String> simpleWords = new HashSet<String>();
		BufferedReader simpleList = null;
		try {
			simpleList = new BufferedReader(new InputStreamReader(
					new URL("http://countwordsworth.com/download/DaleChallEasyWordList.txt").openStream()));
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		String word;
		try {
			while ((word = simpleList.readLine()) != null) {
				simpleWords.add(word);
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		words.retainAll(simpleWords);
		int countsSimpleWords = words.size();
		float pctSimple = countsSimpleWords / wordsInText.length;
		if (pctSimple > DIFFICULT_WORD_THRESHOLD) {
			score = score + ADJUSTMENT;
		}
		return new ResponseEntity<Double>(score, HttpStatus.OK);
	}

	@RequestMapping(value = { "/v1/entities", "entities" }, method = RequestMethod.GET)
	public ResponseEntity<Map<String, Set<String>>> getEntities(@RequestParam(name = "loc") String location) {
		RateLimiter rateLimiter = RateLimiter.create(5);
		rateLimiter.acquire();

		BufferedReader contents = null;
		try {
			contents = new BufferedReader(new InputStreamReader(new URL(location).openStream()));
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		String text = null, line = null;
		try {
			while ((line = contents.readLine()) != null) {
				text = text + line + "\n";
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

		Map<String, Set<String>> map = new HashMap<>();
		String serializedClassifier = this.getClass()
				.getResource("edu/stanford/nlp/models/ner/english.muc.7class.distsim.crf.ser.gz").toString();

		CRFClassifier<CoreLabel> classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
		List<List<CoreLabel>> classify = classifier.classify(text);
		for (List<CoreLabel> coreLabels : classify) {
			for (CoreLabel coreLabel : coreLabels) {

				String word = coreLabel.word();
				String category = coreLabel.get(CoreAnnotations.AnswerAnnotation.class);
				if (!"O".equals(category)) {
					if (map.containsKey(category)) {
						// key is already their just insert
						map.get(category).add(word);
					} else {
						LinkedHashSet<String> temp = new LinkedHashSet<String>();
						temp.add(word);
						map.put(category, temp);
					}
				}
			}
		}
		return new ResponseEntity<Map<String, Set<String>>>(map, HttpStatus.OK);
	}

	@SuppressWarnings("deprecation")
	@RequestMapping(value = { "/hash", "/v1/hash" }, method = RequestMethod.GET)
	public Map<String, String> hash(@RequestParam(name = "uri") String uri) {
		RateLimiter rateLimiter = RateLimiter.create(100);
		rateLimiter.acquire();

		URIBuilder url = new URIBuilder().setHost("api.diffbot.com").setScheme("http").setPath("v3/article")
				.addParameter("url", uri).addParameter("token", "38b9af7246e37abc105314c898d1ed0d");

		Request request = null;
		try {
			request = new Request.Builder().url(url.build().toASCIIString()).build();
			logger.debug(url.build().toASCIIString() + "<=== our complete diffbot request URL");
		} catch (URISyntaxException e2) {
			logger.error(e2.getClass().getName() + " caught, stacktrace to follow", e2);
		}

		Response response = null;
		try {
			response = client.newCall(request).execute();
		} catch (IOException e1) {
			logger.error(e1.getMessage(), e1);
		}
		String text = null;
		try {
			String resp = response.body().string();
			logger.info(resp);
			JSONObject json = new JSONObject(resp);
			text = json.getJSONArray("objects").getJSONObject(0).getString("text");
			if (text == null) {
				text = json.getJSONArray("objects").getJSONObject(0).getString("html");
			}
		} catch (JSONException e) {
			logger.error(e.getClass().getName() + " caught, stacktrace to follow", e);
		} catch (IOException e) {
			logger.error(e.getClass().getName() + " caught, stacktrace to follow", e);
		}
		String hash = Base64.encodeBase64String(DigestUtils.sha1(text));

		Seen newest = new Seen();
		newest.setHash(hash);
		logger.info("seenRepository == null => " + new Boolean(seenRepository == null).toString());
		Map<String, String> ret = new HashMap<>();
		ret.put("exists?", new Boolean(seenRepository.exists(hash)).toString());
		if (!seenRepository.exists(hash)) {
			seenRepository.save(newest);
		}
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(
					"jdbc:mysql://aa106w2ihlwnfld.cwblf8lajcuh.us-west-1.rds.amazonaws.com/ebdb?user=root&password=Dylp-Oid-yUl-e");
		} catch (SQLException e1) {
			e1.printStackTrace();
			logger.error(e1.getClass().getName() + " caught, stacktrace to follow", e1);
		}
		PreparedStatement prepped = null;
		try {
			prepped = conn.prepareStatement("select id from articles where url = ?");
		} catch (SQLException e1) {
			e1.printStackTrace();
			logger.error(e1.getClass().getName() + " caught, stacktrace to follow", e1);
		}
		try {
			prepped.setString(1, uri);
		} catch (SQLException e1) {
			e1.printStackTrace();
			logger.error(e1.getClass().getName() + " caught, stacktrace to follow", e1);
		}
		ResultSet results = null;
		try {
			results = prepped.executeQuery();
		} catch (SQLException e1) {
			e1.printStackTrace();
			logger.error(e1.getClass().getName() + " caught, stacktrace to follow", e1);
		}
		try {
			results.next();
		} catch (SQLException e1) {
			e1.printStackTrace();
			logger.error(e1.getClass().getName() + " caught, stacktrace to follow", e1);
		}
		try {
			ret.put("articleId", results.getString(1));
		} catch (SQLException e1) {
			e1.printStackTrace();
			logger.error(e1.getClass().getName() + " caught, stacktrace to follow", e1);
		}
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error(e.getClass().getName() + " caught, stacktrace to follow", e);
		}
		return ret;
	}

	@RequestMapping(value = { "/schema", "/v1/schema" }, method = RequestMethod.GET)
	public ResponseEntity<byte[]> visualize(@RequestParam(name = "url") String jdbc, HttpServletResponse response) {
		URL jdbcUrl = null;
		try {
			jdbcUrl = new URL(jdbc);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			logger.error(e.getClass().getName() + " caught, stacktrace to follow", e);
		}
		String databaseType = jdbcUrl.getProtocol().split(":")[1];
		String databaseName = jdbcUrl.getFile().substring(jdbcUrl.getFile().indexOf(jdbcUrl.getPath()));
		String databaseHostname = jdbcUrl.getHost();
		String databasePassword = jdbcUrl.getUserInfo().split(":")[1];
		String databaseUser = jdbcUrl.getUserInfo().split(":")[0];
		Integer port = jdbcUrl.getPort();
		List<String> args = new ArrayList<>();
		args.add(String.format("-t %s", databaseType));
		args.add(String.format("-db %s", databaseName));
		args.add(String.format("-dp %s", System.getProperty("jdbc.drivers")));
		if (databaseHostname != null) {
			args.add(String.format("-host %s", databaseHostname));
		} else {
			args.add("-host localhost");
		}

		if (databaseUser == null) {
			databaseUser = System.getProperty("user.name");
		}
		args.add(String.format("-u %s", databaseUser));
		if (databasePassword != null) {
			args.add(String.format("-p %s", databasePassword));
		}
		args.add(String.format("-port %d", port));
		File fl = null;
		try {
			fl = File.createTempFile("schema", ".dot");
		} catch (IOException e1) {
			logger.error(e1.getClass().getName() + " caught, stacktrace to follow", e1);
		}
		args.add("-o " + fl.getAbsolutePath());
		List<Long> l = new ArrayList<>();
		l.add(0L);
		args.add("-hq");
		Runnable r = new Thread() {
			public void run() {
				try {
					Main.main(args.toArray(new String[] {}));
				} catch (Exception e) {
					logger.error(e.getClass().getName() + " caught, stacktrace to follow", e);
				}
			}
		};
		Thread t = new Thread(r);
		t.start();
		try {
			t.join();
		} catch (InterruptedException e1) {
			logger.error(e1.getClass().getName() + " caught, stacktrace to follow", e1);
		}

		FileInputStream bais = null;
		try {
			bais = new FileInputStream(fl);
		} catch (FileNotFoundException e) {
			logger.error(e.getClass().getName() + " caught, stacktrace to follow", e);
		}
		byte[] data = new byte[(int) (fl.length() + 1L)];
		try {
			bais.read(data);
		} catch (IOException e) {
			logger.error(e.getClass().getName() + " caught, stacktrace to follow", e);
		}
		fl.delete();
		response.setContentType("text/vnd.graphviz");
		return new ResponseEntity<>(data, HttpStatus.CREATED);
	}

	@RequestMapping(value = { "/text", "/v1/text" }, method = RequestMethod.GET)
	public ResponseEntity<String> getText(@RequestParam(name = "uri") String uri) {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(uri).newBuilder();
		urlBuilder.addQueryParameter("uri", uri);
		String url = urlBuilder.build().toString();
		Request request = new Request.Builder().url(url).build();

		Response response = null;
		try {
			response = client.newCall(request).execute();
		} catch (IOException e1) {
			logger.error(e1.getMessage(), e1);
		}
		String text = null;
		try {
			text = response.body().string();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		Document soup = Jsoup.parse(text);
		return new ResponseEntity<>(soup.text(), HttpStatus.OK);
	}

	@RequestMapping(value = { "/phrases", "/v1/phrases" }, method = RequestMethod.POST)
	public ResponseEntity<List<String>> tokenize(@RequestBody String body,
			@RequestParam(name = "id") String identitifier) {
		// store the sentence tokenizer once per run
		RateLimiter rateLimiter = RateLimiter.create(17);
		rateLimiter.acquire();

		final List<String> returnValue = new ArrayList<>();
		if (binFile == null) {
			try {
				binFile = File.createTempFile("en-sent", ".bin");
			} catch (IOException e1) {
				logger.error(e1.getClass().getName() + " caught, stacktrace to follow", e1);
			}
			String url = "http://opennlp.sourceforge.net/models-1.5/en-sent.bin";
			Request request = new Request.Builder().url(HttpUrl.parse(url)).build();
			Response response = null;
			try {
				response = client.newCall(request).execute();
			} catch (IOException e1) {
				logger.error(e1.getClass().getName() + " caught, stacktrace to follow", e1);
			}
			try {
				BufferedInputStream bis = new BufferedInputStream(response.body().byteStream());
				IOUtils.copy(bis, new FileOutputStream(binFile));
			} catch (IOException e) {
				logger.error(e.getClass().getName() + " caught, stacktrace to follow", e);
			}
		}
		System.setProperty(SENTENCE_LOCATION_KEY, binFile.getAbsolutePath());
		try {
			Request request = new Request.Builder().url("https://hd1-ner.herokuapp.com/phrases")
					.post(com.squareup.okhttp.RequestBody.create(MediaType.parse("text/plain"), body)).build();
			DriverManagerDataSource dataSource = new DriverManagerDataSource();
			dataSource.setDriverClassName(
					"jdbc:mysql://aa106w2ihlwnfld.cwblf8lajcuh.us-west-1.rds.amazonaws.com/ebdb?user=root");
			dataSource.setPassword("Dylp-Oid-yUl-e");
			dataSource.setUsername("root");

			JdbcTemplate updateTable = new JdbcTemplate(dataSource);
			Response response = client.newCall(request).execute();
			List<String> sentences = new Gson().fromJson(response.body().charStream(), new TypeToken<List<String>>() {
			}.getType());
			for (String s : sentences) {
				returnValue.add(s);
				updateTable.update("INSERT INTO sentences (body, position, article_id) VALUES (?, ?, ?)", s,
						sentences.indexOf(s), identitifier);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return new ResponseEntity<>(returnValue, HttpStatus.OK);
	}

	@Bean
	public User user() {
		return new User();
	}

	@Bean
	public UserRepository getUserRepo() {
		return repository;
	}

	@Bean
	public SeenRepository getSeenRepo() {
		return seenRepository;
	}
}
