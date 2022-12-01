package com.spring.groovy.approval.controller;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import com.spring.groovy.approval.model.ApprovalVO;
import com.spring.groovy.approval.model.BiztripReportVO;
import com.spring.groovy.approval.model.DraftFileVO;
import com.spring.groovy.approval.model.DraftVO;
import com.spring.groovy.approval.model.ExpenseListVO;
import com.spring.groovy.approval.model.OfficialAprvLineVO;
import com.spring.groovy.approval.model.SavedAprvLineVO;
import com.spring.groovy.approval.service.InterApprovalService;
import com.spring.groovy.common.FileManager;
import com.spring.groovy.common.Pagination;
import com.spring.groovy.management.model.MemberVO;

@Controller
@RequestMapping(value = "/approval/*")
public class ApprovalController {

	private InterApprovalService service;
	private FileManager fileManager;

	@Autowired
	public ApprovalController(InterApprovalService service, FileManager fileManager) {
		this.service = service;
		this.fileManager = fileManager;
	}

	// 전자결재 홈 페이지요청
	@RequestMapping(value = "/home.on")
	public ModelAndView approvalHome(ModelAndView mav, HttpServletRequest request) {

		MemberVO loginuser = getLoginUser(request);

		// 결재 대기 문서 개수 알아오기

		// 결재 대기 문서 4개 가져오기

		// 진행 중 문서 개수 알아오기

		// 진행 중 문서 5개 가져오기

		// 결재완료된 문서 5개 가져오기
		List<DraftVO> processedDraftList = service.getMyDraftProcessed(loginuser.getEmpno());

		mav.addObject("processedDraftList", processedDraftList);
		mav.setViewName("approval/home.tiles");

		return mav;
	}
	
	// 기안 문서 조회
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/draftDetail.on")
	public ModelAndView getDraftDetail(ModelAndView mav, HttpServletRequest request, DraftVO dvo) {
		
		Map<String, Object> draftMap = service.getDraftDetail(dvo);
		mav.addObject("draftMap", draftMap);
		
		String fk_draft_type_no = request.getParameter("fk_draft_type_no");
		
		switch (fk_draft_type_no) {
		case "1":
			mav.setViewName("approval/draft_detail/work_detail.tiles");
			break;

		case "2":
			mav.setViewName("approval/draft_detail/expense_detail.tiles");
			break;
		
		case "3":
			mav.setViewName("approval/draft_detail/business_trip_detail.tiles");
			break;

		default:
			mav.setViewName("error");
			break;
		}
		
		// 전체 결재자 정보 리스트
		JSONArray avoList = new JSONArray((List<ApprovalVO>)draftMap.get("avoList"));		
		
		// 내부 결재자 정보 리스트
		JSONArray internalList = new JSONArray((List<ApprovalVO>)draftMap.get("internalList"));
		
		// 외부 결재자 정보 리스트
		JSONArray externalList = new JSONArray((List<ApprovalVO>)draftMap.get("externalList"));
		
		mav.addObject("avoList", String.valueOf(avoList));
		mav.addObject("internalList", String.valueOf(internalList));
		mav.addObject("externalList", String.valueOf(externalList));
		return mav;
	}
	
	// 파일 다운로드
	@ResponseBody
	@RequestMapping(value = "/download.on")
	public void fileDownload(HttpServletRequest request, HttpServletResponse response) {
		
		// 첨부파일 번호
		String draft_file_no = request.getParameter("draft_file_no");
		
		response.setContentType("text/html; charset=UTF-8");
		PrintWriter out = null;

		try {
			DraftFileVO dfvo = service.getAttachedFile(draft_file_no); // 파일 조회
			
			// 글번호가 없거나 파일 이름이 없다면
			if (dfvo == null || (dfvo != null && dfvo.getFilename() == null)) {
				out = response.getWriter();
				out.println("<script type='text/javascript'>alert('존재하지 않는 파일입니다.'); history.back();</script>");
				return;
			}
			
			String filename = dfvo.getFilename(); // 저장된 파일 이름
			String originalFilename = dfvo.getOriginalFilename(); // 원본 파일 이름
			
			// 첨부파일이 저장되어 있는 WAS 서버의 디스크 경로명을 알아온다.
			HttpSession session = request.getSession();
			String root = session.getServletContext().getRealPath("/");
			
			String path = root+"resources"+File.separator+"files";
			
			boolean flag = false;// file 다운로드 성공, 실패를 알려주는 용도
			
			// FileManager의 파일 다운로드 메소드 호출
			flag = fileManager.doFileDownload(filename, originalFilename, path, response);
			
			if (!flag) { // 파일 다운로드 실패 시
				out = response.getWriter();
				out.println("<script type='text/javascript'>alert('파일 다운로드 실패');</script>");
			}
			
		} catch (IOException e) { // 입출력예외가 발생한 경우
			try {
				e.printStackTrace();
				out = response.getWriter();
				out.println("<script type='text/javascript'>alert('파일 다운로드 불가');</script>");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	// 개인문서함-상신함 페이지요청
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/personal/sent.on")
	public ModelAndView sentDraftList(ModelAndView mav, Pagination pagination, HttpServletRequest request) throws Exception {

		MemberVO loginuser = getLoginUser(request);

		Map<String, Object> paraMap = BeanUtils.describe(pagination); // pagination을 Map으로
		paraMap.put("empno", loginuser.getEmpno());

		// 전체 글 개수 구하기
		int listCnt = service.getSentDraftCnt(paraMap);
		pagination.setPageInfo(listCnt); // 총 페이지, 시작행, 마지막행 설정
		paraMap.putAll(BeanUtils.describe(pagination)); // pagination을 Map으로

		// 정렬 설정
		setSorting(request, paraMap);

		// 한 페이지에 표시할 글 목록
		mav.addObject("draftList", service.getSentDraftList(paraMap));

		// 페이지바 생성
		String url = request.getContextPath() + "/approval/personal/sent.on";
		pagination.setQueryString("&sortType="+paraMap.get("sortType")+"&sortOrder="+paraMap.get("sortOrder"));
		mav.addObject("pagebar", pagination.getPagebar(url));
		mav.addObject("paraMap", paraMap);

		mav.setViewName("approval/my_draft/sent.tiles");
		return mav;
	}

	// 개인문서함-결재함 페이지요청
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/personal/processed.on")
	public ModelAndView processdDraftList(ModelAndView mav, Pagination pagination, HttpServletRequest request)
			throws Exception {

		MemberVO loginuser = getLoginUser(request);

		Map<String, Object> paraMap = BeanUtils.describe(pagination); // pagination을 Map으로
		paraMap.put("empno", loginuser.getEmpno());

		// 전체 글 개수 구하기
		int listCnt = service.getProcessedDraftCnt(paraMap);
		pagination.setPageInfo(listCnt); // 총 페이지, 시작행, 마지막행 설정
		paraMap.putAll(BeanUtils.describe(pagination)); // pagination을 Map으로

		// 정렬 설정
		setSorting(request, paraMap);

		// 한 페이지에 표시할 글 목록
		mav.addObject("draftList", service.getProcessedDraftList(paraMap));

		// 페이지바
		String url = request.getContextPath() + "/approval/personal/processed.on";
		pagination.setQueryString("&sortType="+paraMap.get("sortType")+"&sortOrder="+paraMap.get("sortOrder"));
		mav.addObject("pagebar", pagination.getPagebar(url));
		mav.addObject("paraMap", paraMap);

		mav.setViewName("approval/my_draft/processed.tiles");
		return mav;
	}

	// 개인문서함-임시저장함 페이지요청
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/personal/saved.on")
	public ModelAndView savedDraftList(ModelAndView mav, Pagination pagination, HttpServletRequest request) throws Exception {

		MemberVO loginuser = getLoginUser(request);

		Map<String, Object> paraMap = BeanUtils.describe(pagination); // pagination을 Map으로
		paraMap.put("empno", loginuser.getEmpno());

		// 전체 글 개수 구하기
		int listCnt = service.getSavedDraftCnt(paraMap);

		pagination.setPageInfo(listCnt); // 총 페이지, 시작행, 마지막행 설정
		paraMap.putAll(BeanUtils.describe(pagination)); // pagination을 Map으로

		// 정렬 설정
		setSorting(request, paraMap);

		// 한 페이지에 표시할 글 목록
		mav.addObject("tempDraftList", service.getSavedDraftList(paraMap));

		// 페이지바
		String url = request.getContextPath() + "/approval/personal/saved.on";
		pagination.setQueryString("&sortType="+paraMap.get("sortType")+"&sortOrder="+paraMap.get("sortOrder"));
		mav.addObject("pagebar", pagination.getPagebar(url));
		mav.addObject("paraMap", paraMap);

		mav.setViewName("approval/my_draft/saved.tiles");
		return mav;
	}

	// 개인문서함-임시저장함 글삭제
	@ResponseBody
	@PostMapping(value = "/delete.on", produces = "text/plain;charset=UTF-8")
	public String deleteDraftList(ModelAndView mav, HttpServletRequest request) {

		// 지울 파일 목록
		String deleteList = request.getParameter("deleteList");

		String[] deleteArr = deleteList.split(",");

		int result = service.deleteDraftList(deleteArr);

		JSONObject jsonObj = new JSONObject();
		jsonObj.put("result", result);

		return jsonObj.toString();
	}

	// 팀문서함 페이지요청
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/team.on")
	public ModelAndView teamDraftList(ModelAndView mav, Pagination pagination, HttpServletRequest request)
			throws Exception {

		MemberVO loginuser = getLoginUser(request);

		Map<String, Object> paraMap = BeanUtils.describe(pagination); // pagination을 Map으로
		paraMap.put("department", loginuser.getDepartment());

		// 전체 글 개수 구하기
		int listCnt = service.getTeamDraftCnt(paraMap);
		pagination.setPageInfo(listCnt); // 총 페이지, 시작행, 마지막행 설정
		paraMap.putAll(BeanUtils.describe(pagination)); // pagination을 Map으로

		// 정렬 설정
		setSorting(request, paraMap);

		// 한 페이지에 표시할 글 목록
		mav.addObject("draftList", service.getTeamDraftList(paraMap));

		// 페이지바
		String url = request.getContextPath() + "/approval/team.on";
		pagination.setQueryString("&sortType="+paraMap.get("sortType")+"&sortOrder="+paraMap.get("sortOrder"));
		mav.addObject("pagebar", pagination.getPagebar(url));
		mav.addObject("paraMap", paraMap);

		mav.setViewName("approval/team_draft.tiles"); // View
		return mav;
	}

	// 결재하기-결재대기문서 페이지요청
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/requested.on")
	public ModelAndView requestedDraftList(ModelAndView mav, Pagination pagination, HttpServletRequest request)
			throws Exception {
		MemberVO loginuser = getLoginUser(request);

		Map<String, Object> paraMap = BeanUtils.describe(pagination); // pagination을 Map으로
		paraMap.put("empno", loginuser.getEmpno());
		paraMap.put("department", loginuser.getDepartment());

		// 결재 대기 문서의 문서번호들 조회
		List<Object> draftNoList = service.getRequestedDraftNo(paraMap);
		paraMap.put("draftNoList", draftNoList);

		// 만약 대기문서가 없다면 return
		if (draftNoList.size() == 0) {
			mav.setViewName("approval/processing_draft/requested_draft.tiles");
			return mav;
		}

		// 전체 글 개수 구하기
		int listCnt = service.getRequestedDraftCnt(paraMap);
		pagination.setPageInfo(listCnt); // 총 페이지, 시작행, 마지막행 설정
		paraMap.putAll(BeanUtils.describe(pagination)); // pagination을 Map으로

		// 정렬 설정
		setSorting(request, paraMap);

		// 한 페이지에 표시할 글 목록
		mav.addObject("draftList", service.getRequestedDraftList(paraMap));

		// 페이지바
		String url = request.getContextPath() + "/approval/requested.on";
		pagination.setQueryString("&sortType="+paraMap.get("sortType")+"&sortOrder="+paraMap.get("sortOrder"));
		mav.addObject("pagebar", pagination.getPagebar(url));
		mav.addObject("paraMap", paraMap);

		mav.setViewName("approval/processing_draft/requested_draft.tiles");
		return mav;
	}
	
	// 결재하기-결재예정문서 페이지요청
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/upcoming.on")
	public ModelAndView upcomingDraftList(ModelAndView mav, Pagination pagination, HttpServletRequest request)
			throws Exception {
		MemberVO loginuser = getLoginUser(request);
		
		Map<String, Object> paraMap = BeanUtils.describe(pagination); // pagination을 Map으로
		paraMap.put("empno", loginuser.getEmpno());
		paraMap.put("department", loginuser.getDepartment());
		
		// 결재 대기 문서의 문서번호들 조회
		List<Object> draftNoList = service.getUpcomingDraftNo(paraMap);
		paraMap.put("draftNoList", draftNoList);
		
		// 만약 대기문서가 없다면 return
		if (draftNoList.size() == 0) {
			mav.setViewName("approval/processing_draft/upcoming_draft.tiles");
			return mav;
		}
		
		// 전체 글 개수 구하기
		int listCnt = service.getUpcomingDraftCnt(paraMap);
		pagination.setPageInfo(listCnt); // 총 페이지, 시작행, 마지막행 설정
		paraMap.putAll(BeanUtils.describe(pagination)); // pagination을 Map으로
		
		// 정렬 설정
		setSorting(request, paraMap);
		
		// 한 페이지에 표시할 글 목록
		mav.addObject("draftList", service.getUpcomingDraftList(paraMap));
		
		// 페이지바
		String url = request.getContextPath() + "/approval/upcoming.on";
		pagination.setQueryString("&sortType="+paraMap.get("sortType")+"&sortOrder="+paraMap.get("sortOrder"));
		mav.addObject("pagebar", pagination.getPagebar(url));
		mav.addObject("paraMap", paraMap);
		
		mav.setViewName("approval/processing_draft/upcoming_draft.tiles");
		return mav;
	}

	// 기안 작성 페이지요청
	@GetMapping(value = "/write.on")
	public ModelAndView showWorkDraftForm(ModelAndView mav, @RequestParam("type_no") String type_no) {
		
		// 공통 결재라인 가져오기
		List<MemberVO> recipientList = service.getRecipientList(type_no);
		JSONArray recipientArr = new JSONArray(recipientList);
		
		mav.addObject("recipientArr", String.valueOf(recipientArr));
		
		switch (type_no) {
		
		case "1":
			mav.setViewName("approval/write_form/work_form.tiles");
			return mav;

		case "2":
			mav.setViewName("approval/write_form/expense_form.tiles");
			return mav;
		
		case "3":
			mav.setViewName("approval/write_form/business_trip_form.tiles");
			return mav;

		default:
			mav.setViewName("error");
			return mav;
		}
		
	}

	// 기안 작성하기
	@ResponseBody
	@PostMapping(value = "/addDraft.on", produces = "text/plain;charset=UTF-8")
	public String addDraft(MultipartHttpServletRequest mtfRequest, DraftVO dvo, ApprovalVO avo, ExpenseListVO evo, BiztripReportVO brvo) {

		Map<String, Object> paraMap = new HashMap<>();
		
		// 기안 정보
		paraMap.put("dvo", dvo);

		// 결재자 목록 리스트
		List<ApprovalVO> apvoList = avo.getAvoList();
		paraMap.put("apvoList", apvoList);

		// service로 넘길 파일정보가 담긴 리스트
		List<DraftFileVO> fileList = new ArrayList<DraftFileVO>();

		// 첨부파일이 있을 시
		if (mtfRequest.getFiles("fileList").size() > 0) {
			
			// 파일 업로드 경로 지정
			String path = setFilePath(mtfRequest, "files");
			
			// view에서 넘어온 파일들
			List<MultipartFile> multiFileList = mtfRequest.getFiles("fileList");
			
			// 파일 업로드하기
			for (MultipartFile attach : multiFileList) {
				
				String filename = ""; // 저장될 파일명
				String originalFilename = ""; // 원본 파일명
				byte[] bytes = null; // 파일 내용물
				long filesize = 0; // 파일 크기
				
				try {
					// 첨부파일의 내용물을 읽어온다.
					bytes = attach.getBytes();
					
					// originalFilename을 읽어온다.
					originalFilename = attach.getOriginalFilename();
					
					// 새로운 파일명으로 디스크에 저장한다.
					filename = fileManager.doFileUpload(bytes, originalFilename, path);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				DraftFileVO dfvo = new DraftFileVO();
				
				// dfvo set
				dfvo.setFilename(filename);
				dfvo.setOriginalFilename(originalFilename);
				filesize = attach.getSize(); // 첨부파일의 크기 (단위: byte)
				dfvo.setFilesize(String.valueOf(filesize));
				
				fileList.add(dfvo);
			}
		}
		paraMap.put("fileList", fileList);
		
		// 지출내역 리스트
		List<ExpenseListVO> evoList = evo.getEvoList();
		paraMap.put("evoList", evoList);
		
		// 출장보고 정보
		paraMap.put("brvo", brvo);

		boolean result = service.addDraft(paraMap);

		JSONObject jsonObj = new JSONObject();
		jsonObj.put("result", result);

		return jsonObj.toString();
	}

	// 파일 업로드 경로 지정
	private String setFilePath(MultipartHttpServletRequest mtfRequest, String directory) {
		HttpSession session = mtfRequest.getSession();
		String root = session.getServletContext().getRealPath("/");
		String path = root + "resources" + File.separator + directory;
		
		return path;
	}
	
	// 기안 임시저장하기
	@ResponseBody
	@PostMapping(value = "/saveDraft.on", produces = "text/plain;charset=UTF-8")
	public String saveTemp(HttpServletRequest request, DraftVO dvo, ApprovalVO avo) {
		
		Map<String, Object> paraMap = new HashMap<>();

		// 기안 정보
		paraMap.put("dvo", dvo);

		// 결재자 목록 리스트
		List<ApprovalVO> apvoList = new ArrayList<ApprovalVO>();

		String[] empnoArr = request.getParameterValues("fk_approval_empno");
		
		if (empnoArr != null) {
			for (String empno : empnoArr) {
				ApprovalVO apvo = new ApprovalVO();
				apvo.setFk_approval_empno(Integer.parseInt(empno));
	
				apvoList.add(apvo);
			}
		}
		paraMap.put("apvoList", apvoList);
		
		boolean result = service.saveWorkDraft(paraMap);

		JSONObject jsonObj = new JSONObject();
		jsonObj.put("result", result);

		return jsonObj.toString();
	}
	

	// 결재라인 선택 팝업창 요청
	@RequestMapping(value = "/selectApprovalLine.on")
	public ModelAndView selectApprovalLine(ModelAndView mav, HttpServletRequest request) {

		MemberVO loginuser = getLoginUser(request);
		String type = request.getParameter("type");
		
		Map<String, Object> paraMap = new HashMap<>();
		paraMap.put("loginuser", loginuser);
		paraMap.put("type", type);
		
		// 부문 목록 가져오기
		List<Map<String, String>> bumunList = service.getBumunList(paraMap);

		JSONArray bumunArray = new JSONArray();

		for (Map<String, String> map : bumunList) {
			JSONObject json = new JSONObject(map);
			bumunArray.put(json);
		}

		mav.addObject("bumunArray", bumunArray.toString());

		// 부서 목록 가져오기
		List<Map<String, String>> deptList = service.getDeptList(paraMap);

		JSONArray deptArray = new JSONArray();

		for (Map<String, String> map : deptList) {
			JSONObject json = new JSONObject(map);
			deptArray.put(json);
		}

		mav.addObject("deptArray", deptArray.toString());

		// 사원 목록 가져오기
		List<Map<String, String>> empList = service.getEmpList(paraMap);

		JSONArray empArray = new JSONArray();

		for (Map<String, String> map : empList) {
			JSONObject json = new JSONObject(map);
			empArray.put(json);
		}

		mav.addObject("empArray", empArray.toString());

		mav.setViewName("approval/select_approval_member");

		return mav;

	}

	// 저장된 결재라인 목록 불러오기 팝업창 요청
	@ResponseBody
	@RequestMapping(value = "/getSavedAprvLine.on", produces = "text/plain;charset=UTF-8")
	public String getSavedAprvLine(Model model, HttpServletRequest request) {

		MemberVO loginuser = getLoginUser(request);

		Map<String, String> paraMap = new HashMap<>();
		paraMap.put("empno", loginuser.getEmpno());

		// 저장된 결재라인 목록 불러오기
		List<SavedAprvLineVO> aprvLineList = service.getSavedAprvLine(paraMap);

		JSONArray aprvLineArray = new JSONArray();

		for (SavedAprvLineVO vo : aprvLineList) {
			JSONObject json = new JSONObject(vo);
			aprvLineArray.put(json);
		}

		return aprvLineArray.toString();

	}

	// 저장된 결재라인 결재자 정보 가져오기
	@ResponseBody
	@RequestMapping(value = "/getSavedAprvEmpInfo.on", produces = "text/plain;charset=UTF-8")
	public String getSavedAprvEmpInfo(HttpServletRequest request) {

		String param = request.getParameter("selectedAprvLine");

		JSONArray jsonArray = new JSONArray(param);
		JSONObject json = jsonArray.getJSONObject(0);

		// 결재자들의 사원번호를 담을 리스트
		List<String> empnoList = new ArrayList<>();

		for (int i = 1; i < 4; i++) {
			String searchKey = "fk_approval_empno" + i;
			if (json.has(searchKey))
				empnoList.add(String.valueOf(json.get(searchKey)));
		}

		// 결재자들의 정보가 검색되어 담긴 리스트
		List<MemberVO> aprvEmpInfo = service.getSavedAprvEmpInfo(empnoList);

		JSONArray aprvArray = new JSONArray(aprvEmpInfo);

		return aprvArray.toString();

	}
	
	// 자신의 결재 처리하기(승인 or 반려)
	@ResponseBody
	@PostMapping(value = "/updateApproval.on", produces = "text/plain;charset=UTF-8")
	public String updateApproval(ApprovalVO avo) {
		
		boolean result = service.updateApproval(avo);

		JSONObject jsonObj = new JSONObject();
		jsonObj.put("result", result);

		return jsonObj.toString();
	}

	// 환경설정-결재라인관리 페이지요청
	@RequestMapping(value = "/config/approvalLine.on")
	public ModelAndView configApprovalLine(ModelAndView mav, HttpServletRequest request) {
		MemberVO loginuser = getLoginUser(request);

		Map<String, String> paraMap = new HashMap<>();
		paraMap.put("empno", loginuser.getEmpno());
		
		// 저장된 결재라인 목록 불러오기
		List<SavedAprvLineVO> aprvLineList = service.getSavedAprvLine(paraMap);
		
		mav.addObject("aprvLineList", aprvLineList);
		mav.setViewName("approval/config/approvalLine.tiles");
		return mav;
	}
	
	// 환경설정-저장된 결재라인 한개 불러오기
	@ResponseBody
	@RequestMapping(value = "/admin/getOneAprvLine.on", produces = "text/plain;charset=UTF-8")
	public String getOneAprvLine(HttpServletRequest request) {

		String aprv_line_no = request.getParameter("aprv_line_no");

		List<MemberVO> savedAprvLine = service.getOneAprvLine(aprv_line_no); 

		JSONArray aprvArray = new JSONArray();

		for (MemberVO emp : savedAprvLine) {
			JSONObject json = new JSONObject(emp);
			aprvArray.put(json);
		}

		return aprvArray.toString();
		
	}

	// 환경설정-결재라인 추가 페이지요청
	@RequestMapping(value = "/config/approvalLine/add.on")
	public String addApprovalLine(HttpServletRequest request) {

		return "approval/config/approvalLine_add.tiles";
	}

	// 환경설정-결재라인 저장
	@PostMapping(value = "/config/approvalLine/save.on")
	public ModelAndView saveApprovalLine(ModelAndView mav, HttpServletRequest request, SavedAprvLineVO sapVO) {

		// insert
		int n = service.saveApprovalLine(sapVO);

		if (n == 0) {
			mav.addObject("message", "결재라인 저장에 실패하였습니다.");
			mav.addObject("loc", "javascript:history.back()");
		} else {
			mav.addObject("message", "결재라인이 저장되었습니다.");
			mav.addObject("loc", request.getContextPath() + "/approval/config/approvalLine.on");
		}

		mav.setViewName("msg");

		return mav;
	}
	
	// 환경설정-결재라인 수정
	@PostMapping(value = "/config/approvalLine/edit.on")
	public ModelAndView editApprovalLine(ModelAndView mav, HttpServletRequest request, SavedAprvLineVO sapVO) {
		
		// update
		int n = service.editApprovalLine(sapVO);
		
		if (n == 0) {
			mav.addObject("message", "결재라인 저장에 실패하였습니다.");
			mav.addObject("loc", "javascript:history.back()");
		} else {
			mav.addObject("message", "결재라인이 저장되었습니다.");
			mav.addObject("loc", request.getContextPath() + "/approval/config/approvalLine.on");
		}
		
		mav.setViewName("msg");
		
		return mav;
	}
	
	// 환경설정-결재라인 삭제
	@PostMapping(value = "/config/approvalLine/del.on")
	public ModelAndView delApprovalLine(ModelAndView mav, HttpServletRequest request, SavedAprvLineVO sapVO) {
		
		// delete
		int n = service.delApprovalLine(sapVO);
		
		if (n == 0) {
			mav.addObject("message", "결재라인 삭제에 실패하였습니다.");
			mav.addObject("loc", "javascript:history.back()");
		} else {
			mav.addObject("message", "결재라인이 삭제되었습니다.");
			mav.addObject("loc", request.getContextPath() + "/approval/config/approvalLine.on");
		}
		
		mav.setViewName("msg");
		
		return mav;
	}

	// 환경설정-서명관리 페이지요청
	@RequestMapping(value = "/config/signature.on")
	public String configSignature(HttpServletRequest request) {

		return "approval/config/signature.tiles";
	}
	
	// 환경설정-서명이미지 수정
	@RequestMapping(value = "/config/signature/update.on")
	public ModelAndView updateSignature(ModelAndView mav, MultipartHttpServletRequest mtfRequest) {
		
		// 파일 업로드 경로 지정
		String path = setFilePath(mtfRequest, "images" + File.separator + "sign");
		
		// view에서 넘어온 파일들
		MultipartFile attach = mtfRequest.getFile("attach");
		
		// 파일 업로드하기
		String filename = "";
		String originalFilename = ""; // 원본 파일명
		byte[] bytes = null; // 파일 내용물
		
		try {
			// 첨부파일의 내용물을 읽어온다.
			bytes = attach.getBytes();
			
			// originalFilename을 읽어온다.
			originalFilename = attach.getOriginalFilename();
			
			// 새로운 파일명으로 디스크에 저장한다.
			filename = fileManager.doFileUpload(bytes, originalFilename, path);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		MemberVO loginuser = getLoginUser(mtfRequest);
						
		Map<String, String> paraMap = new HashMap<String, String>();
		paraMap.put("filename", filename);
		paraMap.put("empno", loginuser.getEmpno());
		
		// update
		int n = service.updateSignature(paraMap);

		if (n == 0) {
			mav.addObject("message", "서명 이미지 수정에 실패하였습니다.");
			mav.addObject("loc", "javascript:history.back()");
		} else {
			// loginuser signimg필드 바꿔주기
			loginuser.setSignimg(filename);
			
			mav.addObject("message", "서명 이미지가 수정되었습니다.");
			mav.addObject("loc", mtfRequest.getContextPath() + "/approval/config/signature.on");
		}

		mav.setViewName("msg");
		
		return mav;
	}
	
	// 관리자메뉴-공통결재라인 설정 페이지요청
	@RequestMapping(value = "/admin/officialApprovalLine.on")
	public ModelAndView getOfficialApprovalLine(ModelAndView mav) {
		
		// 공통결재라인 목록 불러오기
		List<Map<String, String>> officialAprvList = service.getOfficialAprvList(); 
		
		mav.addObject("officialAprvList", officialAprvList);
		mav.setViewName("approval/admin/official_approvalLine.tiles");
		return mav;
	}
	
	// 관리자메뉴-공통결재라인 한개 불러오기
	@ResponseBody
	@RequestMapping(value = "/admin/getOneOfficialAprvLine.on", produces = "text/plain;charset=UTF-8")
	public String getOneOfficialAprvLine(HttpServletRequest request) {

		String official_aprv_line_no = request.getParameter("official_aprv_line_no");

		List<MemberVO> officialAprvLine = service.getOneOfficialAprvLine(official_aprv_line_no); 

		JSONArray aprvArray = new JSONArray();

		for (MemberVO emp : officialAprvLine) {
			JSONObject json = new JSONObject(emp);
			aprvArray.put(json);
		}

		return aprvArray.toString();
		
	}
	
	// 관리자메뉴-공통결재라인 수정
	@PostMapping(value = "/admin/approvalLine/save.on")
	public ModelAndView saveOfficialApprovalLine(ModelAndView mav, HttpServletRequest request, OfficialAprvLineVO oapVO) {

		// update
		int n = service.saveOfficialApprovalLine(oapVO);

		if (n == 0) {
			mav.addObject("message", "결재라인 저장에 실패하였습니다.");
			mav.addObject("loc", "javascript:history.back()");
		} else {
			mav.addObject("message", "결재라인이 저장되었습니다.");
			mav.addObject("loc", request.getContextPath() + "/approval/admin/officialApprovalLine.on");
		}

		mav.setViewName("msg");

		return mav;
	}
	
	// 로그인 사용자 정보 가져오기
	private MemberVO getLoginUser(HttpServletRequest request) {
		HttpSession session = request.getSession();
		MemberVO loginuser = (MemberVO) session.getAttribute("loginuser");
		return loginuser;
	}

	// 문서함 조회 시 정렬 설정하기
	private void setSorting(HttpServletRequest request, Map<String, Object> paraMap) {
		// 정렬기준
		String sortType = request.getParameter("sortType");
		sortType = sortType == null ? "draft_date" : sortType;
		paraMap.put("sortType", sortType);

		// 정렬순서
		String sortOrder = request.getParameter("sortOrder");
		sortOrder = sortOrder == null ? "desc" : sortOrder;
		paraMap.put("sortOrder", sortOrder);
	}

	@ExceptionHandler(Exception.class)
	private String error(Exception e) {
		e.printStackTrace();
		return "error";
	}

	// 문서함 목록 엑셀 다운로드
	@RequestMapping(value = "excel/downloadExcelFile.on")
	public String downloadExcelFile(Model model, HttpServletRequest request) {

		Enumeration<String> params = request.getParameterNames();

		HashMap<String, Object> map = new HashMap<String, Object>();
		while (params.hasMoreElements()) {
			String key = params.nextElement().toString();
			String value = request.getParameter(key);

			map.put(key, value);
		}

		// 다운로드할 글 목록
		String downloadList = String.valueOf(map.get("downloadList"));
		String[] downloadArray = downloadList.split(",");

		// 다운로드할 문서함 이름
		String listName = String.valueOf(map.get("listName"));

		// 헤더 셀에 들어갈 이름 목록
		String header = String.valueOf(map.get("header"));
		String[] headerArray = header.split(",");
		int length = headerArray.length;

		// 워크북 객체 생성
		SXSSFWorkbook workbook = new SXSSFWorkbook();

		// 시트생성
		SXSSFSheet sheet = workbook.createSheet(listName);

		// 행의 위치를 나타내는 변수
		int rowLocation = 0;

		// CellStyle 정렬(Alignment)
		CellStyle mergeRowStyle = workbook.createCellStyle(); // 셀 병합 스타일
		mergeRowStyle.setAlignment(HorizontalAlignment.CENTER);
		mergeRowStyle.setVerticalAlignment(VerticalAlignment.CENTER);

		CellStyle headerStyle = workbook.createCellStyle(); // 헤더스타일
		headerStyle.setAlignment(HorizontalAlignment.CENTER);
		headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

		// CellStyle 배경색(ForegroundColor)
		mergeRowStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
		mergeRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		headerStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		// CellStyle 데이터 포맷 설정하기
		CellStyle moneyStyle = workbook.createCellStyle();
		moneyStyle.setDataFormat(HSSFDataFormat.getBuiltinFormat("#,##0")); // 천단위 쉼표

		// Cell 폰트(Font) 설정하기
		Font mergeRowFont = workbook.createFont();
		mergeRowFont.setFontName("나눔고딕");
		mergeRowFont.setFontHeight((short) 500);
		mergeRowFont.setColor(IndexedColors.WHITE.getIndex());
		mergeRowFont.setBold(true);
		mergeRowStyle.setFont(mergeRowFont);

		// CellStyle 테두리 Border
		headerStyle.setBorderTop(BorderStyle.THICK);
		headerStyle.setBorderBottom(BorderStyle.THICK);
		headerStyle.setBorderLeft(BorderStyle.THIN);
		headerStyle.setBorderRight(BorderStyle.THIN);

		// Cell Merge 셀 병합시키기

		// 병합할 행 만들기
		Row mergeRow = sheet.createRow(rowLocation); // 0번째 행

		// 병합할 행에 셀 스타일 주기
		for (int i = 0; i < length; i++) {
			Cell cell = mergeRow.createCell(i);
			cell.setCellStyle(mergeRowStyle);
			cell.setCellValue(listName);
		}

		// 셀 병합하기 => 첫 번째 행을 병합한다.
		sheet.addMergedRegion(new CellRangeAddress(rowLocation, rowLocation, 0, length - 1)); // 시작 행, 끝 행, 시작 열, 끝 열

		// 헤더 행 생성
		Row headerRow = sheet.createRow(++rowLocation);

		// 헤더 열 생성
		for (int i = 0; i < length; i++) {
			Cell headerCell = headerRow.createCell(i);
			headerCell.setCellValue(headerArray[i]);
			headerCell.setCellStyle(headerStyle);
		}

		// 내용에 해당하는 행 및 셀 생성하기
		Row bodyRow = null;
		Cell bodyCell = null;

		// 반복횟수
		int loop = (int) Math.ceil((downloadArray.length / (length * (1.0))));
		for (int i = 0, j = 0; i < loop; i++) {

			// 행생성
			bodyRow = sheet.createRow(i + (rowLocation + 1));

			// 열 생성
			for (int k = 0; k < length; k++) {
				bodyCell = bodyRow.createCell(k);
				bodyCell.setCellValue(downloadArray[j++]);
			}

		}

		sheet.trackAllColumnsForAutoSizing();
		// 열 너비 설정
		for (int i = 0; i < length; i++) {
			sheet.autoSizeColumn(i);
			sheet.setColumnWidth(i, (sheet.getColumnWidth(i)) + 700);
		}

		model.addAttribute("locale", Locale.KOREA);
		model.addAttribute("workbook", workbook);
		model.addAttribute("workbookName", listName);

		return "excelDownloadView"; // 엑셀 다운로드 뷰 역할을 해주는 bean
	}
}

