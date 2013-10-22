<?php
	$cookie_name = "chalmersItAuth";
	if (!isset($_COOKIE[$cookie_name])) {
		// header("Location: http://beta.chalmers.it/login");
		die('var user = undefined;');
	}
	$url =  "https://chalmers.it/auth/userInfo.php?token=" . $_COOKIE[$cookie_name];

	$user_json = file_get_contents($url);
	if ($user_json == "invalid token")
		$user_json = "null";
	header("Content-Type: application/javascript");
?>
var user = <?=$user_json?>;
