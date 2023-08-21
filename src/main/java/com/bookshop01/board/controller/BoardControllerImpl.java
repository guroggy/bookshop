package com.bookshop01.board.controller;

import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import com.bookshop01.board.service.BoardService;
import com.bookshop01.board.vo.ArticleVO;
import com.bookshop01.member.vo.MemberVO;

@Controller("boardController")
public class BoardControllerImpl implements BoardController {

	private static final Logger logger = LoggerFactory.getLogger(BoardControllerImpl.class);

	private static final String ARTICLE_IMAGE_REPO = "C:\\board\\article_image";

	@Autowired
	private BoardService boardService;
	@Autowired
	private ArticleVO articleVO;

	HttpSession session = null;

	@Override
	@RequestMapping(value = "/board/listArticles.do", method = { RequestMethod.GET, RequestMethod.POST })
	public ModelAndView listArticles(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String viewName = (String) request.getAttribute("viewName");
		List articlesList = boardService.listArticles();
		ModelAndView mav = new ModelAndView(viewName);
		
		HttpSession session = request.getSession();
		session = request.getSession();
		MemberVO vo = (MemberVO) session.getAttribute("memberInfo");
		session.setAttribute("id", vo.getMember_id());
		logger.info("이것은 아티클리스트 객체" + articlesList);
		mav.addObject("articlesList", articlesList);
		return mav;

	}

	@RequestMapping(value = "/board/replyForm.do", method = RequestMethod.GET)
	private ModelAndView form2(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String viewName = (String) request.getAttribute("viewName");

		int parentNO = Integer.parseInt(request.getParameter("parentNO"));
		session = request.getSession();

		session.setAttribute("parentNO", parentNO);

		logger.info("부모글" + parentNO);
		ModelAndView mav = new ModelAndView();
		mav.setViewName(viewName);
		return mav;
	}

	@Override
	@RequestMapping(value = { "/board/addReply.do" }, method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity addReply(MultipartHttpServletRequest multipartRequest, HttpServletResponse response)
			throws Exception {
		Map<String, Object> articleMap = new HashMap<String, Object>();
		Enumeration enu = multipartRequest.getParameterNames();
		while (enu.hasMoreElements()) {
			String name = (String) enu.nextElement();
			String value = multipartRequest.getParameter(name);
			logger.info("답글 추가시 파라미터 이름 : " + name + "파라미터 값:" +  value);
			articleMap.put(name, value);
		}

		String imageFileName = upload(multipartRequest);
		logger.info("답글 추가시 이미지 파일명 : " + imageFileName );

		// �빐�떦 �슂泥��뿉 ���븳 �꽭�뀡�뿉�꽌 �쉶�썝(member)媛��졇�삤怨� �빐�떦 硫ㅻ쾭�쓽 id媛��졇���꽌 �꽕�젙
		HttpSession session = multipartRequest.getSession();

		MemberVO memberVO = (MemberVO) session.getAttribute("memberInfo");
		logger.info("답글 사는 사람의 id" + memberVO.getMember_id());

		int parentNO = (Integer) session.getAttribute("parentNO");

		logger.info("답글의 부모글 번호 : " + parentNO);
		String id = memberVO.getMember_id();
		articleMap.put("parentNO", parentNO);
		articleMap.put("id", memberVO.getMember_id());
		articleMap.put("imageFileName", imageFileName);

		String message;
		ResponseEntity resEnt = null;
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "text/html; charset=utf-8");

		try {

			int articleNO = boardService.addNewArticle(articleMap);
			logger.info("새글 추가 후 글번호" + articleNO);
			if (imageFileName != null && imageFileName.length() != 0) {
				File srcFile = new File(ARTICLE_IMAGE_REPO + "\\" + "temp" + "\\" + imageFileName);
				File destDir = new File(ARTICLE_IMAGE_REPO + "\\" + articleNO);
				FileUtils.moveFileToDirectory(srcFile, destDir, true);
			}

			message = "<script>";
			message += " alert('답글을 추가하였습니다.');";
			message += " location.href='" + multipartRequest.getContextPath() + "/board/listArticles.do'; ";
			message += " </script>";
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);

		} catch (Exception e) {
			File srcFile = new File(ARTICLE_IMAGE_REPO + "\\" + "temp" + "\\" + imageFileName);
			srcFile.delete();

			message = " <script>";
			message += " alert('오류가 발생했습니다. 다시 시도해 주세요');');";
			message += " location.href='" + multipartRequest.getContextPath() + "/board/articleForm.do'; ";
			message += " </script>";
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
			e.printStackTrace();
		}

		return resEnt;
	}

	@Override
	@RequestMapping(value = { "/board/addNewArticle.do" }, method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity addNewArticle(MultipartHttpServletRequest multipartRequest, HttpServletResponse response)
			throws Exception {

		multipartRequest.setCharacterEncoding("utf-8");
		Map<String, Object> articleMap = new HashMap<String, Object>();
		Enumeration enu = multipartRequest.getParameterNames();
		while (enu.hasMoreElements()) {
			String name = (String) enu.nextElement();
			String value = multipartRequest.getParameter(name);
			articleMap.put(name, value);
		}

		String imageFileName = upload(multipartRequest);
		HttpSession session = multipartRequest.getSession();

		MemberVO memberVO = (MemberVO) session.getAttribute("memberInfo");
		String id = memberVO.getMember_id();
		articleMap.put("parentNO", 0);
		articleMap.put("id", id);
		articleMap.put("imageFileName", imageFileName);

		String message;
		ResponseEntity resEnt = null;
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "text/html; charset=utf-8");
		try {
			int articleNO = boardService.addNewArticle(articleMap);
			if (imageFileName != null && imageFileName.length() != 0) {
				File srcFile = new File(ARTICLE_IMAGE_REPO + "\\" + "temp" + "\\" + imageFileName);
				File destDir = new File(ARTICLE_IMAGE_REPO + "\\" + articleNO);
				FileUtils.moveFileToDirectory(srcFile, destDir, true);
			}

			message = "<script>";
			message += " alert('새글을 추가했습니다.');";
			message += " location.href='" + multipartRequest.getContextPath() + "/board/listArticles.do'; ";
			message += " </script>";
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
		} catch (Exception e) {
			File srcFile = new File(ARTICLE_IMAGE_REPO + "\\" + "temp" + "\\" + imageFileName);
			srcFile.delete();

			message = " <script>";
			message += " alert('오류가 발생했습니다. 다시 시도해 주세요);');";
			message += " location.href='" + multipartRequest.getContextPath() + "/board/articleForm.do'; ";
			message += " </script>";
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
			e.printStackTrace();
		}
		return resEnt;
	}

	// �븳媛쒖쓽 �씠誘몄� 蹂댁뿬二쇨린
	@RequestMapping(value = "/board/viewArticle.do", method = RequestMethod.GET)
	public ModelAndView viewArticle(@RequestParam("articleNO") int articleNO, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String viewName = (String) request.getAttribute("viewName");
		articleVO = boardService.viewArticle(articleNO);
		ModelAndView mav = new ModelAndView();
		mav.setViewName(viewName);
		mav.addObject("article", articleVO);
		return mav;
	}

	/*
	 * //�떎以� �씠誘몄� 蹂댁뿬二쇨린
	 * 
	 * @RequestMapping(value="/board/viewArticle.do" ,method = RequestMethod.GET)
	 * public ModelAndView viewArticle(@RequestParam("articleNO") int articleNO,
	 * HttpServletRequest request, HttpServletResponse response) throws Exception{
	 * String viewName = (String)request.getAttribute("viewName"); Map
	 * articleMap=boardService.viewArticle(articleNO); ModelAndView mav = new
	 * ModelAndView(); mav.setViewName(viewName); mav.addObject("articleMap",
	 * articleMap); return mav; }
	 */

	// �븳 媛� �씠誘몄� �닔�젙 湲곕뒫
	@RequestMapping(value = "/board/modArticle.do", method = { RequestMethod.POST, RequestMethod.GET })
	@ResponseBody
	public ResponseEntity modArticle(MultipartHttpServletRequest multipartRequest, HttpServletResponse response)
			throws Exception {
		multipartRequest.setCharacterEncoding("utf-8");
		Map<String, Object> articleMap = new HashMap<String, Object>();
		Enumeration enu = multipartRequest.getParameterNames();
		while (enu.hasMoreElements()) {
			String name = (String) enu.nextElement();
			String value = multipartRequest.getParameter(name);
			articleMap.put(name, value);
		}

		String imageFileName = upload(multipartRequest);
		articleMap.put("imageFileName", imageFileName);

		String articleNO = (String) articleMap.get("articleNO");
		String message;
		ResponseEntity resEnt = null;
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "text/html; charset=utf-8");
		try {
			boardService.modArticle(articleMap);
			if (imageFileName != null && imageFileName.length() != 0) {
				File srcFile = new File(ARTICLE_IMAGE_REPO + "\\" + "temp" + "\\" + imageFileName);
				File destDir = new File(ARTICLE_IMAGE_REPO + "\\" + articleNO);
				FileUtils.moveFileToDirectory(srcFile, destDir, true);

				String originalFileName = (String) articleMap.get("originalFileName");
				File oldFile = new File(ARTICLE_IMAGE_REPO + "\\" + articleNO + "\\" + originalFileName);
				oldFile.delete();
			}
			message = "<script>";
			message += " alert('글을 수정했습니다.');";
			message += " location.href='" + multipartRequest.getContextPath() + "/board/viewArticle.do?articleNO="
					+ articleNO + "';";
			message += " </script>";
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
		} catch (Exception e) {
			File srcFile = new File(ARTICLE_IMAGE_REPO + "\\" + "temp" + "\\" + imageFileName);
			srcFile.delete();
			message = "<script>";
			message += " alert('오류가 발생했습니다.다시 수정해주세요');";
			message += " location.href='" + multipartRequest.getContextPath() + "/board/viewArticle.do?articleNO="
					+ articleNO + "';";
			message += " </script>";
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
		}
		return resEnt;
	}

	@Override
	@RequestMapping(value = "/board/removeArticle.do", method = { RequestMethod.POST, RequestMethod.GET })
	@ResponseBody
	public ResponseEntity removeArticle(@RequestParam("articleNO") int articleNO, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		response.setContentType("text/html; charset=UTF-8");
		String message;
		ResponseEntity resEnt = null;
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Content-Type", "text/html; charset=utf-8");
		try {
			boardService.removeArticle(articleNO);
			File destDir = new File(ARTICLE_IMAGE_REPO + "\\" + articleNO);
			FileUtils.deleteDirectory(destDir);

			message = "<script>";
			message += " alert('글을 삭제했습니다');";
			message += " location.href='" + request.getContextPath() + "/board/listArticles.do';";
			message += " </script>";
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);

		} catch (Exception e) {
			message = "<script>";
			message += " alert('오류가 발생했습니다.다시 수정해주세요.');";
			message += " location.href='" + request.getContextPath() + "/board/listArticles.do';";
			message += " </script>";
			resEnt = new ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
			e.printStackTrace();
		}
		return resEnt;
	}

	/*
	 * //�떎以� �씠誘몄� 湲� 異붽��븯湲�
	 * 
	 * @Override
	 * 
	 * @RequestMapping(value="/board/addNewArticle.do" ,method = RequestMethod.POST)
	 * 
	 * @ResponseBody public ResponseEntity addNewArticle(MultipartHttpServletRequest
	 * multipartRequest, HttpServletResponse response) throws Exception {
	 * multipartRequest.setCharacterEncoding("utf-8"); String imageFileName=null;
	 * 
	 * Map articleMap = new HashMap(); Enumeration
	 * enu=multipartRequest.getParameterNames(); while(enu.hasMoreElements()){
	 * String name=(String)enu.nextElement(); String
	 * value=multipartRequest.getParameter(name); articleMap.put(name,value); }
	 * 
	 * //濡쒓렇�씤 �떆 �꽭�뀡�뿉 ���옣�맂 �쉶�썝 �젙蹂댁뿉�꽌 湲��벖�씠 �븘�씠�뵒瑜� �뼸�뼱���꽌 Map�뿉 ���옣�빀�땲�떎. HttpSession session =
	 * multipartRequest.getSession(); MemberVO memberVO = (MemberVO)
	 * session.getAttribute("member"); String id = memberVO.getId();
	 * articleMap.put("id",id);
	 * 
	 * 
	 * List<String> fileList =upload(multipartRequest); List<ImageVO> imageFileList
	 * = new ArrayList<ImageVO>(); if(fileList!= null && fileList.size()!=0) {
	 * for(String fileName : fileList) { ImageVO imageVO = new ImageVO();
	 * imageVO.setImageFileName(fileName); imageFileList.add(imageVO); }
	 * articleMap.put("imageFileList", imageFileList); } String message;
	 * ResponseEntity resEnt=null; HttpHeaders responseHeaders = new HttpHeaders();
	 * responseHeaders.add("Content-Type", "text/html; charset=utf-8"); try { int
	 * articleNO = boardService.addNewArticle(articleMap); if(imageFileList!=null &&
	 * imageFileList.size()!=0) { for(ImageVO imageVO:imageFileList) { imageFileName
	 * = imageVO.getImageFileName(); File srcFile = new
	 * File(ARTICLE_IMAGE_REPO+"\\"+"temp"+"\\"+imageFileName); File destDir = new
	 * File(ARTICLE_IMAGE_REPO+"\\"+articleNO); //destDir.mkdirs();
	 * FileUtils.moveFileToDirectory(srcFile, destDir,true); } }
	 * 
	 * message = "<script>"; message += " alert('�깉湲��쓣 異붽��뻽�뒿�땲�떎.');"; message +=
	 * " location.href='"+multipartRequest.getContextPath()
	 * +"/board/listArticles.do'; "; message +=" </script>"; resEnt = new
	 * ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
	 * 
	 * 
	 * }catch(Exception e) { if(imageFileList!=null && imageFileList.size()!=0) {
	 * for(ImageVO imageVO:imageFileList) { imageFileName =
	 * imageVO.getImageFileName(); File srcFile = new
	 * File(ARTICLE_IMAGE_REPO+"\\"+"temp"+"\\"+imageFileName); srcFile.delete(); }
	 * }
	 * 
	 * 
	 * message = " <script>"; message +=" alert('�삤瑜섍� 諛쒖깮�뻽�뒿�땲�떎. �떎�떆 �떆�룄�빐 二쇱꽭�슂');');";
	 * message +=" location.href='"+multipartRequest.getContextPath()
	 * +"/board/articleForm.do'; "; message +=" </script>"; resEnt = new
	 * ResponseEntity(message, responseHeaders, HttpStatus.CREATED);
	 * e.printStackTrace(); } return resEnt; }
	 * 
	 */

	@RequestMapping(value = "/board/articleForm.do", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView articleForm( HttpServletRequest request, HttpServletResponse response) throws Exception {
		String viewName = (String) request.getAttribute("viewName");
		
		HttpSession session= request.getSession();
		MemberVO memberVO=(MemberVO) session.getAttribute("memberInfo");
		
		String _id = memberVO.getMember_id();
//		logger.info("_id�쓽 媛�" + _id);

		ModelAndView mav = new ModelAndView(viewName);
		
		mav.addObject("_id", _id);
		
		return mav;

	}

	@RequestMapping(value = "/board/*Form.do", method = { RequestMethod.POST, RequestMethod.GET })
	private ModelAndView form(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String viewName = (String) request.getAttribute("viewName");
		ModelAndView mav = new ModelAndView();
		mav.setViewName(viewName);

		return mav;
	}

	// �븳媛� �씠誘몄� �뾽濡쒕뱶�븯湲�
	private String upload(MultipartHttpServletRequest multipartRequest) throws Exception {
//		logger.info("�뿬湲곕뒗 upload 硫붿꽌�뱶 �쁺�뿭");
		String imageFileName = null;
		Iterator<String> fileNames = multipartRequest.getFileNames();
//		logger.info("硫��떚�뙆�듃由ы�섏뒪�듃媛� 媛��졇�삩  �뙆�씪 �쟾泥� 媛앹껜" + fileNames);

		while (fileNames.hasNext()) {
			String fileName = fileNames.next();
//			logger.info("硫��떚�뙆�듃由ы�섏뒪�듃媛� 媛��졇�삩  �뙆�씪 媛앹껜" + fileName);

			/*
			 * MultipartFile 硫��떚�뙆�듃 �슂泥��뿉�꽌 �닔�떊�맂 �뾽濡쒕뱶�맂 �뙆�씪�쓽 �몴�쁽�엯�땲�떎.
			 * 
			 * �뙆�씪 �궡�슜�� 硫붾え由ъ뿉 ���옣�릺嫄곕굹 �뵒�뒪�겕�뿉 �엫�떆濡� ���옣�맗�땲�떎. �몢 寃쎌슦 紐⑤몢 �궗�슜�옄�뒗 �썝�븯�뒗 寃쎌슦 �뙆�씪 �궡�슜�쓣 �꽭�뀡 �닔以� �삉�뒗 �쁺援� ���옣�냼�뿉
			 * 蹂듭궗�빐�빞 �빀�땲�떎. �엫�떆 ���옣�냼�뒗 �슂泥� 泥섎━媛� �걹�굹硫� 吏��썙吏묐땲�떎.
			 */
			MultipartFile multipartFile = multipartRequest.getFile(fileName);
			imageFileName = multipartFile.getOriginalFilename();
//			logger.info("硫��떚�뙆�듃由ы�섏뒪�듃媛� 媛��졇�삩  �씠誘몄� �뙆�씪 紐�" + imageFileName);
			File file = new File(ARTICLE_IMAGE_REPO + "\\" + fileName);
//			logger.info("�뙆�씪 媛앹껜:" + file);

//			logger.info("硫��뵾�뙆�듃�뙆�씪�슜�웾:" + multipartFile.getSize());

			if (multipartFile.getSize() != 0) { // File Null Check
//				logger.info("�뙆�씪 議댁옱 �뿬遺� " + file.exists()); // �씠 �떆�젏�쓽 �뙆�씪 寃쎈줈�뿉�뒗 �뙆�씪�씠 �뾾�쓬
				if (!file.exists()) { // 寃쎈줈�긽�뿉 �뙆�씪�씠 議댁옱�븯吏� �븡�쓣 寃쎌슦
//					logger.info("�뵒�젆�넗由� 留뚮뱾�뿀�뒗吏� �뿬遺� " + file.getParentFile().mkdirs()); // 寃쎈줈�뿉 �빐�떦�븯�뒗 �뵒�젆�넗由щ뱾�쓣 �깮�꽦

					if (file.getParentFile().mkdirs()) { // 寃쎈줈�뿉 �빐�떦�븯�뒗 �뵒�젆�넗由щ뱾�쓣 �깮�꽦
						file.createNewFile(); // �씠�썑 �뙆�씪 �깮�꽦
					}
				}
				multipartFile.transferTo(new File(ARTICLE_IMAGE_REPO + "\\" + "temp" + "\\" + imageFileName)); // �엫�떆濡�
																												// ���옣�맂
																												// multipartFile�쓣
																												// �떎�젣
																												// �뙆�씪濡�
																												// �쟾�넚
			}
		}
		return imageFileName;
	}

	/*
	 * //�떎以� �씠誘몄� �뾽濡쒕뱶�븯湲� private List<String> upload(MultipartHttpServletRequest
	 * multipartRequest) throws Exception{ List<String> fileList= new
	 * ArrayList<String>(); Iterator<String> fileNames =
	 * multipartRequest.getFileNames(); while(fileNames.hasNext()){ String fileName
	 * = fileNames.next(); MultipartFile mFile = multipartRequest.getFile(fileName);
	 * String originalFileName=mFile.getOriginalFilename();
	 * fileList.add(originalFileName); File file = new File(ARTICLE_IMAGE_REPO
	 * +"\\"+ fileName); if(mFile.getSize()!=0){ //File Null Check if(!
	 * file.exists()){ //寃쎈줈�긽�뿉 �뙆�씪�씠 議댁옱�븯吏� �븡�쓣 寃쎌슦 if(file.getParentFile().mkdirs()){
	 * //寃쎈줈�뿉 �빐�떦�븯�뒗 �뵒�젆�넗由щ뱾�쓣 �깮�꽦 file.createNewFile(); //�씠�썑 �뙆�씪 �깮�꽦 } }
	 * mFile.transferTo(new File(ARTICLE_IMAGE_REPO
	 * +"\\"+"temp"+ "\\"+originalFileName)); //�엫�떆濡� ���옣�맂 multipartFile�쓣 �떎�젣 �뙆�씪濡� �쟾�넚 } }
	 * return fileList; }
	 */

//	@RequestMapping("/board/addNewArticle2.do")
//	public String getMemberId(HttpServletRequest request) throws Exception {
//		
////		1. 留ㅽ띁�뿉 寃쎈줈 異붽��븯湲� // �뻽�쑝硫� �뿬湲� 硫붿꽌�뱶�뿉 �젒洹� �븯�뒗吏� �솗�씤�븯湲�
////		2. �깮媛곹빐 蹂대땲 �븘�떚�겢 id �뱾怨좎삤硫� �븞 �맖
//		
//		request.setAttribute("member_id", articleVO.getId());
//		System.out.println(articleVO.getId() + " �씠嫄� 萸먮쑉吏�");
//		
//		
////		http://localhost:8070/kingshop/board/articleForm.do
//		return "";
//	}

}
