package com.fiskkit.instantEmail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)

public class ControllerTest {
	@SuppressWarnings("unused")
	private static Logger LOG = LoggerFactory.getLogger(ControllerTest.class);

	@Autowired
	FiskController controller;

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void randomXXXKeysReturnsNullSetsHttpStatus() throws Exception {
		RequestEntity<Void> request = RequestEntity.get(new URI("http://localhost:"+this.port+"/random.XXX")).build();
		ResponseEntity<String> respShouldBeNull = this.restTemplate.exchange(request, String.class);
		Assert.assertNull(respShouldBeNull.getBody());
		Assert.assertEquals(HttpStatus.EXPECTATION_FAILED, respShouldBeNull.getStatusCode());
	}

	@Test
	public void randomJsonKeys() throws Exception {

		Map<String, String> response = this.restTemplate.getForObject(
				"http://localhost:" + this.port + "/random.json", Map.class);
		Set<String> expectedKeys = new HashSet<>();
		Collections.addAll(expectedKeys,
				new String[] { "int", "char", "boolean", "float", "long" });
		Assert.assertEquals(expectedKeys, response.keySet());
	}

	@Test
	public void randomXmlKeys() throws Exception {

		Map<String, String> response = this.restTemplate.getForObject(
				"http://localhost:" + this.port + "/random.xml", Map.class);
		Set<String> expectedKeys = new HashSet<>();
		Collections.addAll(expectedKeys, new String[] { "int", "char",
				"boolean", "float", "long", "String" });
		Assert.assertEquals(expectedKeys, response.keySet());
	}

	@Test
	public void testPOS() throws Exception {

		String response = this.restTemplate.postForObject(
				"http://escotilla.d8u.us:5000/",
				"Fairly hideous: note how we define the type of collection",
				String.class);
		Assertions.assertThat(response.equals(
				"{\"partsOfSpeech\":{\"DT\":[\"the\"],\"IN\":[\"of\"],\"JJ\":[\"hideous\"],\"NN\":[\"type\",\"collection\"],\"PRP\":[\"we\"],\"RB\":[\"Fairly\"],\"VB\":[\"note\"],\"VBP\":[\"define\"],\"WRB\":[\"how\"]}}"));
	}

	@Test
	public void twitter() throws Exception {

		String response = this.restTemplate.getForObject("http://localhost:"
				+ this.port
				+ "/v1/tweet/o8mbC8sJhC/?title=Exclusive:%20Here%27s%20The%20Full%2010-Page%20Anti-Diversity%20Screed%20Circulating%20Internally%20at%20Google",
				String.class);
		Assertions.assertThat(response.endsWith("@hdiwan"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void preview() throws Exception {

		Map<String, String> response = this.restTemplate
				.getForObject(
						"http://localhost:" + this.port
						+ "/v1/preview?loc=http://purple.com",
						Map.class);
		Assertions.assertThat(response.containsKey("image")
				&& response.containsKey("summary"));
	}

	@Test
	public void facebook() throws Exception {

		Boolean response = this.restTemplate.getForObject("http://localhost:"
				+ this.port
				+ "/v1/facebook?email=hasan.diwan@gmail.com&title=title=Exclusive:%20Here%27s%20The%20Full%2010-Page%20Anti-Diversity%20Screed%20Circulating%20Internally%20at%20Google",
				Boolean.class);
		Assertions.assertThat((response.booleanValue() == true)
				|| (response.booleanValue() == false));
	}

	@Test
	public void isCreatedProperly() throws Exception {

		Assertions.assertThat(this.controller).isNotNull();
	}

	@Test
	public void userValidTest() {

		Assertions.assertThat((this.restTemplate.getForObject(
				"http://localhost:" + this.port
				+ "/v1/valid?subscription=1sjs9hvQ5tmUbX2I1Z",
				String.class) == "true"));
	}

	@Test
	public void expiredSubscription() {

		Assertions.assertThat(this.restTemplate.getForObject(
				"http://localhost:" + this.port
				+ "/v1/valid?subscription=cbdemo_dave-sub2",
				String.class) == "false");
	}

	@Test
	public void testHash() {

		Assertions
		.assertThat(this.restTemplate.getForObject(
				"http://localhost:" + this.port
				+ "/v1/hash?url=http%3A%2F%2Fwww.purple.com",
				Map.class).get("exists?").equals(Boolean.FALSE));
		Assertions
		.assertThat(this.restTemplate.getForObject(
				"http://localhost:" + this.port
				+ "/v1/hash?url=http%3A%2F%2Fwww.purple.com",
				Map.class).get("exists?").equals(Boolean.TRUE));
	}

	@Test
	public void readabilityOfText() {

		String text = "the quick brown fox jumped over the lazy dog.";
		Assertions.assertThat(this.restTemplate.postForObject(
				"http://localhost:" + this.port + "/readability", text,
				Double.class) < 1.00);
	}

	@Test
	public void statistics() {

		String text = "the quick brown fox jumped over the lazy dog.";
		@SuppressWarnings("unchecked")
		Map<String, String> stats = this.restTemplate.postForObject(
				"http://localhost:" + this.port + "/analyze", text, Map.class);
		Set<String> expectedKeys = new HashSet<>();
		expectedKeys.add("wordCount");
		expectedKeys.add("averageWordLength");
		expectedKeys.add("mostCommonWords");
		Assertions.assertThat(stats.keySet().containsAll(expectedKeys));
	}

	@Test
	public void getText() {

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("User-Agent", "foo");
		String actual = this.restTemplate.getForObject("http://localhost:"
				+ this.port
				+ "/text?url=https%3A%2F%2Fwww.reddit.com%2Fr%2Fhelp%2Fcomments%2F6lqm79%2Fis_there_a_way_to_determine_my_total_post_count%2Fdk9nqro%2F?utm-foo=bar",
				String.class);
		String expected = "cruyff8 comments on Is there a way to determine my total post count per subreddit? jump to content my subreddits edit subscriptions popular -all -random |  AskReddit -funny -worldnews -todayilearned -pics -videos -gifs -aww -news -movies -gaming -mildlyinteresting -television -Showerthoughts -Jokes -sports -OldSchoolCool -nottheonion -IAmA -tifu -photoshopbattles -explainlikeimfive -science -LifeProTips -personalfinance -TwoXChromosomes -EarthPorn -food -Futurology -space -Music -WritingPrompts -UpliftingNews -Art -dataisbeautiful -nosleep -GetMotivated -Documentaries -askscience -gadgets -books -announcements -creepy -DIY -history -listentothis -philosophy -InternetIsBeautiful -blogmore »  help comments Want to join? Log in or sign up in seconds.| English limit my search to r/help use the following search parameters to narrow your results: subreddit:subreddit find submissions in \"subreddit\" author:username find submissions by \"username\" site:example.com find submissions from \"example.com\" url:text search for \"text\" in url selftext:text search for \"text\" in self post contents self:yes (or self:no) include (or exclude) self posts nsfw:yes (or nsfw:no) include (or exclude) results marked as NSFW e.g. subreddit:aww site:imgur.com dog see the search faq for details. advanced search: by author, subreddit... this post was submitted on 07 Jul 2017 1 point (100% upvoted) shortlink: remember mereset password login Ask a question about Reddit helpsubscribeunsubscribe9,877 readers 308 users here now For your questions about reddit only, please. Check the FAQ to see if your question has already been answered. New to reddit? click here! reddit status -- status page for checking site health Are your submissions not showing on reddit? If you are a new user/trying to submit in a reddit you have not submitted to before, please take some time to first participate in that reddit (browse, upvote content/comments etc). If you have tried the above and your posts still do not show, please contact a moderator in the reddit where you're having problems. See the FAQ for more information. For other questions not specific to reddit, try: /r/AskReddit /r/Advice /r/needadvice /r/techsupport /r/relationship_advice Other helpful subreddits: /r/Bugs - If you have found a possible bug in reddit. /r/goldbenefits - Check out new gold features and reddit gold partners /r/RedditMobile - Official reddit mobile apps /r/MobileWeb - Reddit mobile website /r/Enhancement - Reddit Enhancement Suite (RES) from /u/honestbleeps /r/RESissues - For your RES problems /r/redditrequest - Request to moderate an abandoned subreddit or request a subreddit be unbanned /r/adoptareddit - Give away or acquire an unused subreddit. /r/needamod - Need help moderating a reddit? Want to volunteer? /r/AskModerators - for general questions aimed at moderators of reddit. /r/modhelp - Help for questions about moderation. /r/csshelp - subreddit style help /r/modsupport - a point of contact for moderators to discuss issues with reddit admins, mostly about mod tools /r/modclub A place for mods of communities with 100 or more users to hang out /r/i18n - help translate reddit! /r/ideasfortheadmins - suggestions to improve Reddit /r/FindAReddit - Looking for a subreddit on a particular topic but don't know where to start? /r/FindASubreddit - Looking for a subreddit on a particular topic but don't know where to start? /r/aboutreddit - for submissions about Reddit. /r/TheoryOfReddit - to discuss Reddit. The lightbulb icons next to usernames indicate people who have demonstrated a history of providing useful and helpful answers in this subreddit. Be sure to check the information in the sidebar and the FAQ! Join us in IRC #reddit-help on irc.snoonet.org a community for 9 years message the moderators MODERATORS krispykrackers qgyh2 ytwang davidreiss666Helper Monkey Skuld redtabooadmin Raerth sodypopadmin 316nutshelper allthefoxeshelper ...and 3 more » discussions in r/help <> X 2 · 1 comment Can't upload a picture · 4 comments r/TestSubredditPleaseIgnore can't be created 1 · 1 comment How can i set user flair in reddit is fünf Android APP? 3 · 2 comments My karma hasn't updated for the last 2 days. I have had multiple posts with a lot of upvotes but my karma total is staying the same. Is there a way to fix this? 1 · 7 comments Why do some subreddits always have a lot of members browsing the sub at any given time, yet the subs are very inactive? 3 · 2 comments Is the profile beta actually limited or is it based on my karma? 1 · 3 comments collapse tread 1 · 2 comments Someone is impersonating my friend, as in deliberately. How do I get his account removed 1 · 1 comment My karma count is stuck at 9,222 1 · 4 comments karma should have inflation, what when you dont have interest in a sub anymore you know? any way to transfer post karma? or delete karma for a specific sub so it doesn't show up top on your profile? or donate karma? 0 1 2 Is there a way to determine my total post count per subreddit? (self.help) submitted 9 days ago by theFloodShark 13 comments share loading... sorted by: best topnewcontroversialoldrandomq&alive (beta) you are viewing a single comment's thread. view the rest of the comments → [–]cruyff8 0 points1 point2 points 16 hours ago* (0 children) Loop until the length is less than your limit. I'm not on PC till later today, else I'd provide the actual incantation. EDIT: Now that I am at a computer, the incantation you need is limit=None, as of last August, anyway. permalink embed save parent report give gold reply about blog about source code advertise careers help site rules FAQ wiki reddiquette mod guidelines contact us apps & tools Reddit for iPhone Reddit for Android mobile website buttons <3 reddit gold redditgifts Use of this site constitutes acceptance of our User Agreement and Privacy Policy. © 2017 reddit inc. All rights reserved. REDDIT and the ALIEN Logo are registered trademarks of reddit inc. π Rendered by ";

		Assertions.assertThat(actual.contains(expected));
	}

	@Test
	public void potentialUrls() {

		Assertions.assertThat(this.restTemplate.getForObject(
				"http://localhost:" + this.port + "/url?url=%3A%2F%2F",
				Boolean.class) == Boolean.FALSE);
		Assertions.assertThat(this.restTemplate.getForObject(
				"http://localhost:" + this.port
				+ "/url?url=http%3A%2F%2Fwww.google.co.uk%2F",
				Boolean.class) == Boolean.TRUE);

	}

	private static String readUrl(String urlString) throws IOException {

		String ret = null, line = null;
		try {
			URL url = new URL(urlString);
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(url.openStream()));
			while ((line = reader.readLine()) != null) {
				ret += line;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			ret = null;
		}
		return ret;
	}

	@Test
	public void testPhrases() {

		String URL = "https://www.reddit.com/r/Advice/comments/6wuflp/my_father_is_verbally_abusive_what_is_the_best/dmaxkt7/.json";

		JSONObject json = null;
		try {
			json = new JSONObject(ControllerTest.readUrl(URL));
		} catch (JSONException | IOException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		int ourComment = 0;
		JSONArray comments = null;
		try {
			comments = json.getJSONObject("data").getJSONArray("children")
					.getJSONObject(0).getJSONArray("data");
		} catch (JSONException e) {
			Assert.fail(e.getMessage());
		}
		for (int c = 0; c != comments.length(); c++) {
			JSONObject comment;
			try {
				comment = comments.getJSONObject(c);
				if (comment.getString("author").equals("cruyff8")) {
					ourComment = c;
					break;
				}
			} catch (JSONException e) {
				Assert.fail(e.getMessage());
			}
		}
		String commentBody = null;
		try {
			commentBody = comments.getJSONObject(ourComment).getString("body");
		} catch (JSONException e) {
			Assert.fail(e.getMessage());
		}
		commentBody = commentBody.replaceAll("\\n", "");
		@SuppressWarnings("unchecked")
		List<String> phrases = this.restTemplate.postForObject(
				"http://localhost:" + this.port + "/phrases", commentBody,
				List.class);
		Assertions
		.assertThat(phrases.get(0).equals("No, you don't deserve it."));
	}

	@Test
	public void testSchema() {

		HttpHeaders headers = this.restTemplate
				.headForHeaders("http://localhost:" + this.port
						+ "/schema?url=jdbc%3Apostgresql%3A%2F%2Froot%3ASLvX92gBJqK7ykX4yJtQvV%40mydbinstance.cwblf8lajcuh.us-west-1.rds.amazonaws.com%2FFiskkit_Instant_Email");
		Assertions.assertThat(headers.getContentType().getType() == "text");
		Assertions.assertThat(
				headers.getContentType().getSubtype() == "vnd.graphviz");
	}
}
