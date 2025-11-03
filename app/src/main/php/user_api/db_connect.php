<?php
$connect = new mysqli("localhost", "root", "", "sttherese_db");

if ($connect->connect_error) {
    die("Connection failed: " . $connect->connect_error);
}
?>