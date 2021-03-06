package kr.co.mingyeong.member.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.co.mingyeong.member.model.Member;
import kr.co.mingyeong.member.model.MemberSearch;
import kr.co.mingyeong.member.service.IMemberService;

@Controller
public class MemberController {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Resource(name = "MemberService")
	private IMemberService service;

	// 인덱스
	@RequestMapping("/")
	public ModelAndView index(ModelAndView modelAndView, HttpServletRequest req, HttpServletResponse resp) {
		Member loginInfo = (Member) req.getSession().getAttribute("loginInfo");
		log.debug("loginInfo => {}", loginInfo);
		log.debug("request.getServletContext() => {}", req.getServletContext());
		Boolean login = false;
		RedirectView rdv = new RedirectView();
		try {
			login = (Boolean) req.getSession().getAttribute("login");
			if (login) {
				modelAndView.setViewName("index");
			} else {
				rdv.setUrl("/login");
				modelAndView.setView(rdv);
			}
		} catch (NullPointerException e) {
			// TODO 로그인 유무 익셉션
			rdv.setUrl("/login");
			modelAndView.setView(rdv);
		} catch (Exception e) {
			// TODO: handle exception;
			e.printStackTrace();
			rdv.setUrl("/login");
			modelAndView.setView(rdv);
		}

		log.debug("로그인 유무 => {}", login);

		return modelAndView;
	}

	// 로그인페이지
	@RequestMapping(value = "/login")
	public ModelAndView logIn(ModelAndView modelAndView, @RequestParam HashMap<String, Object> params,
			HttpServletRequest req) {
		log.debug("loginInfo => {}", String.valueOf(req.getSession().getAttribute("loginInfo")));

		if (StringUtils.isEmpty(req.getSession().getAttribute("loginInfo"))) { // 로그인 정보가 없다면
			modelAndView.setViewName("login/login");
		} else { // 로그인되어있다면
			RedirectView rdv = new RedirectView();
			rdv.setUrl("/");
			modelAndView.setView(rdv);
		}
		;
		return modelAndView;
	}

	// 로그인 처리
	@RequestMapping(value = "/loginProc", method = RequestMethod.POST)
	public ModelAndView logInProd(ModelAndView modelAndView, Member params, HttpServletRequest req,
			HttpServletResponse resp) {
		log.debug("logInProd => {}", params.toString());

		RedirectView rdv = new RedirectView();
		try {
			Member loginInfo = service.login(params);
			if (StringUtils.isEmpty(loginInfo)) { // 로그인 실패
				req.getSession().setAttribute("login", false);
				rdv.setUrl("/login");
				modelAndView.setView(rdv);
				modelAndView.addObject("message", "아이디와 비밀번호를 확인해 주세요");
			} else { // 로그인 성공
				req.getSession().setAttribute("login", true);
				req.getSession().setAttribute("loginInfo", loginInfo);
				rdv.setUrl("/");
				modelAndView.setView(rdv);
			}
		} catch (Exception e) {
			// TODO loginProc 익셉션
			e.printStackTrace();
			log.error(e.getMessage(), 2);
			rdv.setUrl("/login");
			modelAndView.setView(rdv);
		}
		return modelAndView;
	}

	// 로그아웃
	@RequestMapping(value = "/logout")
	public ModelAndView logOut(ModelAndView modelAndView, HttpServletRequest req, HttpServletResponse resp) {

		req.getSession().setAttribute("login", false);
		req.getSession().setAttribute("loginInfo", null);
		RedirectView rdv = new RedirectView();
		rdv.setUrl("/login");
		modelAndView.setView(rdv);
		return modelAndView;
	}

	// 회원가입 폼
	@RequestMapping(value = "/join")
	public ModelAndView join(ModelAndView modelAndView) {
		modelAndView.setViewName("login/join");
		return modelAndView;
	}

	// 회원가입
	@RequestMapping(value = "/joinProc", method = RequestMethod.POST)
	@ResponseBody
	public HashMap<String, Object>  insertMember(@RequestBody Member member, HttpServletRequest req) {
		log.debug("join =>{}", member.toString());
		HashMap<String, Object> result = new HashMap<>();
		
		try {
			service.insertMember(member);
			result.put("checkID",true);
			result.put("message","아이디가 장상 등록 완료.");
		} catch (Exception e) {
			// TODO 회원가입 익셉션
			result.put("checkID",false);
			result.put("message",e.getMessage());
			e.printStackTrace();
		}
		return result;
	}

	// 아이디 체크
	@RequestMapping(value = "/checkId", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object>  checkId(@RequestBody HashMap<String, Object> params, Model model) {
		
		log.debug("params =>{}", params.get("mem_id"));
		log.debug("params1 =>{}", params);
		log.debug("model =>{}", model);
		//log.debug("req =>{}", req.getParameter("mem_id"));
//		RedirectView rdv = new RedirectView();
		HashMap<String, Object> result = new HashMap<>();
		
		try {
			int checkId = service.checkID(params);
			if(checkId>=1) {

				result.put("checkID",false);
				result.put("message","아이디가 중복되었습니다.");
				result.put("member",params);
			}else {
				result.put("checkID",true);
				result.put("message","사용가능한 ID입니다");
			}
		} catch (Exception e) {
			// TODO 아이디 체크 익셉션
			result.put("checkID",false);
			result.put("message",e.getMessage());
			e.printStackTrace();
		}
		return result;
	}	
	
	
	// 회원리스트
	@RequestMapping("/memberList")
	public ModelAndView memberList(ModelAndView modelAndView, @ModelAttribute("search") MemberSearch memberSearch,
			HttpServletRequest request) {
		// 한페이지목록
		memberSearch.setListSize(10);
		memberSearch.setPageSize(5);
		memberSearch.setting(service.getMemberCount(memberSearch));
		log.debug("memberlist => {}", memberSearch.toString());
		List<Member> result = service.getMemberList(memberSearch);
		log.debug("result <= {}", result);

		modelAndView.addObject("result", result);
		modelAndView.setViewName("member/memberList");
		return modelAndView;
	}

	// 회원상세보기
	@RequestMapping("/memberView")
	public ModelAndView memberview(ModelAndView modelAndView, Member params, HttpServletRequest request) {
		try {
			if (StringUtils.isEmpty(params.getMem_id())) {
				modelAndView.addObject("msg", "아이디 값이 없음");
			} else {
				Member result = service.getMember(params);
				log.debug("result <= {}", result);
				modelAndView.addObject("result", result);
				modelAndView.setViewName("member/memberView");
			}
		} catch (Exception e) {
			// TODO 회원상세 익셉션
			e.printStackTrace();
		}
		return modelAndView;
	}

	// 관리자가 회원 삭제
	@RequestMapping(value = "/memberDeleteAdmin")
	public ModelAndView adminDelete(ModelAndView modelAndView, Member params) {
		log.debug("adminDelete => {}", params.toString());
		try {
			service.adminDelete(params);
			RedirectView rdv = new RedirectView();
			rdv.setUrl("/memberList");
			modelAndView.setView(rdv);
		} catch (Exception e) {
			// TODO 관리자 회원 삭제 익셉션
			e.printStackTrace();
		}
		return modelAndView;
	}
}
