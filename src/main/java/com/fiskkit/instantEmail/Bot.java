package com.fiskkit.instantEmail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fiskkit.instantEmail.models.User;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

public class Bot {
	private static final Logger LOG = LoggerFactory.getLogger(Bot.class);

	@Autowired
	UserRepository userRepo;

	public void sendMessages() {

		String url = "http://feeds.bbci.co.uk/news/rss.xml";
		SyndFeed feed = null;
		try {
			feed = new SyndFeedInput().build(new XmlReader(new URL(url)));
		} catch (IllegalArgumentException e) {
			Bot.LOG.error(e.getMessage(), e);
		} catch (MalformedURLException e) {
			Bot.LOG.error(e.getMessage(), e);
		} catch (FeedException e) {
			Bot.LOG.error(e.getMessage(), e);
		} catch (IOException e) {
			Bot.LOG.error(e.getMessage(), e);
		}
		for (SyndEntry entry : feed.getEntries()) {
			Iterable<User> usersWithChatId = this.userRepo.findAll();
			for (User u : usersWithChatId) {
				if (u.getChatId() == null) {
					continue;
				}

				if (entry.getPublishedDate().after(u.getLastLogin())) {
					Map<String, String> body = new HashMap<>();
					body.put("chat_id", u.getChatId().toString());
					String text = entry.getTitle() + " @ " + entry.getLink();
					body.put("text", text);
					HttpUrl endpoint = new HttpUrl.Builder()
							.addQueryParameter("chat_id",
									u.getChatId().toString())
							.addQueryParameter("text", text).build();
					Request request = new Request.Builder().url(endpoint)
							.build();
					try {
						new OkHttpClient().newCall(request).execute();
					} catch (IOException e) {
						Bot.LOG.error(e.getMessage(), e);
					}
					u.setLastLogin(entry.getPublishedDate());
				}
			}
		}
	}

	public void onUpdateReceived(long chat_id) throws java.io.IOException {

		Map<String, String> body = new HashMap<>();
		body.put("chat_id", Long.toString(chat_id));
		body.put("text", "Thanks for subscribing");
		HttpUrl endpoint = new HttpUrl.Builder()
				.addQueryParameter("chat_id", this.getBotToken())
				.addQueryParameter("text", "Thanks for subscribing").build();
		Request request = new Request.Builder().url(endpoint).build();
		String response = new OkHttpClient().newCall(request).execute().body()
				.string();
		Bot.LOG.info(response); // TODO complete
	}

	public String getBotUsername() {

		return "FiskkitBot";
	}

	public String getBotToken() {

		return "969556564:AAFFxkfTuFd1CJLujH7rzaVlmHitymSM9PY";
	}

}
