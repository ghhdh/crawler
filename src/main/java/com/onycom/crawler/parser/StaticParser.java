package com.onycom.crawler.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.onycom.common.Util;
import com.onycom.crawler.core.Crawler;
import com.onycom.crawler.data.Duplicate;
import com.onycom.crawler.data.Work;

/**
 * Get URL 방식의 정적 웹페이지를 파싱하는 구현체
 */
public class StaticParser extends Parser {
	
	public StaticParser(){
		
	}
	
	@Override
	public List<Work> parseURL(Work urlInfo, Document document) {
		Map<String, Work> map = new HashMap<String, Work>();
		List<Work> ret = new ArrayList<Work>();
		boolean allow = false;
		
		Work newInfo;
		String href, domain_url, sub_url, url;
		String[] tmp;
		
		int curDepth = urlInfo.getDepth(); 
		
		if(ifLeaf(urlInfo)){
			return ret;
		}
		// ****************************
		// url 중복에 대한 커스텀도 필요할 것으로 보임
		// ****************************
		Elements els = document.select("a[href]");
		for(Element e : els){
			href = e.attr("href").trim();
			if(href.length() == 0) continue;
			tmp = Util.SplitDomainAndSubURL(urlInfo, href);
			domain_url = tmp[0];
			sub_url = tmp[1];
			url = domain_url + sub_url;
			allow = isAllow(urlInfo, domain_url, sub_url);
			
			if(allow){
				newInfo = map.get(url); 
				if(newInfo == null){
					newInfo = new Work(url).setDepth(++curDepth);
					map.put(url, newInfo);
					ret.add(newInfo);
				}
			}else{
//				System.err.println(">> href - " + href);
				//Crawler.DB.insertFilter(urlInfo.getURL(), Util.ConvertForString(href));
//				System.err.println(">> href - " + href);
//				System.err.println(domain_url +" - "+ sub_url);
				//ret.add(new URLInfo(url));
			}
		}
		return ret;
	}
	
	public boolean isAllow(Work curUrlInfo, String targetDomain, String targetSub){
		boolean ret = false;
		List<String> aryFilterAllow = mConfig.getFilterAllow();
		List<String> aryFilterDisallow = mConfig.getFilterDisallow();
		
		String url = targetDomain + targetSub;
		for(String filter : aryFilterAllow){
			if(filter.matches(Parser.REGEX_START_HTTP)){
				if(url.matches(filter)){
					ret = true; break;
				}
			}else{
				if(targetDomain.contentEquals(curUrlInfo.getDomainURL())){
					if(targetSub.matches(filter)){
						ret = true; break;
					}
				}
			}
		}
		
		for(String filter : aryFilterDisallow){
			if(filter.matches(Parser.REGEX_START_HTTP)){
				if(url.matches(filter)){
					ret = false; break;
				}
			}else{
				if(targetDomain.contentEquals(curUrlInfo.getDomainURL())){
					if(targetSub.matches(filter)){
						ret = false; break;
					}
				}
			}
		}
		return ret;
	}
	
	public boolean ifLeaf(Work urlInfo){
		if(mConfig.CRAWLING_MAX_DEPTH != -1){
			if(mConfig.CRAWLING_MAX_DEPTH < urlInfo.getDepth()){
				return true;
			}
		}
		
		List<String> aryLeafURL = mConfig.getLeafURL();
		for(String filter : aryLeafURL){
			if(filter.matches(Parser.REGEX_START_HTTP)){
				if(urlInfo.getURL().matches(filter)){
//					System.err.println(">> leaf - " + urlInfo.getURL());
					//Crawler.DB.insertErr(urlInfo.getURL(), "leaf" );
					return true;
				}
			}else{
				if(urlInfo.getSubURL().matches(filter)){
//					System.err.println(">> leaf - " + urlInfo.getURL());
					//Crawler.DB.insertErr(urlInfo.getURL(), "leaf" );
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public List<Work> checkDupliate(Work[] aryHistory, List<Work> aryNewUrl) {
		String historyUrl;
		boolean isDuplicate = false;
		Work newURLInfo;
		String regex;
		int cut = 0;
		List<Duplicate> aryFilterDuplicate = mConfig.getFilterDuplicate();
		//mFilterDuplicate
		for(int i = aryNewUrl.size()-1 ; i >= 0 ; i--){
			newURLInfo = aryNewUrl.get(i);
			isDuplicate = false;
			for(Work history: aryHistory){
//				
//				if(newURLInfo.getURL().contentEquals(history.getURL())){
//					isDuplicate = true;
//					break;
//				}
//				newUrl = newURLInfo.getURL();
				historyUrl = history.getURL();
				for(Duplicate dup : aryFilterDuplicate){
					// filter 를 newURL 기준으로 만든다
					// 
					regex = dup.regex(newURLInfo);
//					System.out.println(historyUrl);
//					System.out.println("   regex >>" + regex);
					if(regex == null) continue;
					if(historyUrl.matches(regex)){
						isDuplicate = true;
					}
					break;
				}
				if(isDuplicate){
					break;
				}
			}
			if(isDuplicate){
				cut ++;
				//System.out.println(newURLInfo.getURL());
				aryNewUrl.remove(i);
			}else{
				Crawler.DB.writeHistory(newURLInfo.getURL());
			}
		}
		return aryNewUrl;
	}

}
