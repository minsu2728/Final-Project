<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<style>
#list {
	font-size: small;
	margin-bottom: 150px !important;
}

tbody > tr:hover{
	background-color: #E3F2FD;
	cursor: pointer;
}

#pageList a{
	font-size: small;
	color: black;
}

#pageList .active a{
	color: white;
	background-color: #086BDE;
}

#searchBtn {
	border-style: none;
	padding: 3px 7px;
}
</style>
<script>
	$('a#requestedList').css('color','#086BDE');
</script>

<div style='margin: 1% 0 5% 1%'>
	<h4>결재하기</h4>
</div>

<div id='list' class='m-4'>
	
	<h5 class='mb-3'>결재 대기 문서</h5>
	<h6 class='mb-3'>결재해야 할 문서가 <span style='color:#086BDE'>7</span>건 있습니다.</h6>

	<div class="text-right mb-3">
		<form name="searchFrm">
			<%-- 검색 구분 --%>
			<select id="searchType" name="searchType" class="mr-1">
				<option value="draftNo">문서번호</option>
				<option value="draftCate">종류</option>
				<option value="draftSubject">제목</option>
			</select>
			<%-- 검색어 입력창 --%>
			<input type="text" style="display: none;" /> 
			<input type="text" id="searchWord" name="searchWord" placeholder="검색어를 입력하세요" />&nbsp;
			<button class="rounded" type="button" onclick="goSearch()" id='searchBtn'>
				<i class="fas fa-search"></i>
			</button>
		</form>
	</div>

	<div class="row mb-3">
		<div class='col'>
			<i class="fas fa-download"></i>&nbsp;목록 다운로드
		</div>
		<div class='col text-right'>
			<select id="sizePerPage" name="sizePerPage">
					<option value="10">10</option>
					<option value="30">30</option>
					<option value="50">50</option>
			</select> 
			<select id="sortType" name="sortType">
				<option value="newest">최신순</option>
				<option value="oldest">오래된순</option>
			</select>
		</div>
	</div>

	<table class="table">
		<thead>
			<tr class='row'>
				<th class='col'>기안일</th>
				<th class='col'>종류</th>
				<th class='col'>문서번호</th>
				<th class='col col-4'>제목</th>
				<th class='col'>기안자</th>
			</tr>
		</thead>
		<tbody>
			<tr class='row'>
				<td class='col'>2022.11.09</td>
				<td class='col'>지출결의</td>
				<td class='col'>20221109-04</td>
				<td class='col col-4'>시내교통비</td>
				<td class='col'>강택시</td>
			</tr>
		</tbody>
	</table>
</div>

<div id="pageList">
	<!-- Center-aligned -->
	<ul class="pagination justify-content-center" style="margin:20px 0">
	    <li class="page-item"><a class="page-link" href="#"><i class="fas fa-chevron-left"></i></a></li>
		<li class="page-item active"><a class="page-link" href="#">1</a></li>
		<li class="page-item"><a class="page-link" href="#">2</a></li>
		<li class="page-item"><a class="page-link" href="#">3</a></li>
		<li class="page-item"><a class="page-link" href="#"><i class="fas fa-chevron-right"></i></a></li>
	</ul>
</div>