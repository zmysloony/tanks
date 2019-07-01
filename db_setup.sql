-- phpMyAdmin SQL Dump
-- version 4.8.5
-- https://www.phpmyadmin.net/
--
-- Host: localhost
-- Generation Time: Jul 01, 2019 at 03:13 PM
-- Server version: 8.0.13-4
-- PHP Version: 7.2.19-0ubuntu0.18.04.1

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `44MpGSOxW4`
--

-- --------------------------------------------------------

--
-- Table structure for table `games`
--

CREATE TABLE `games` (
  `gid` int(8) NOT NULL,
  `p1_id` int(5) NOT NULL,
  `p2_id` int(5) NOT NULL,
  `seed` int(3) NOT NULL,
  `server_ip` text COLLATE utf8_unicode_ci NOT NULL,
  `server_port` int(4) NOT NULL,
  `state` enum('OPEN','IN_PROGRESS') COLLATE utf8_unicode_ci NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `games_history`
--

CREATE TABLE `games_history` (
  `gid` int(8) NOT NULL,
  `p1_id` int(5) NOT NULL,
  `p2_id` int(5) NOT NULL,
  `seed` int(3) NOT NULL,
  `winner` int(5) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `games_history`
--

INSERT INTO `games_history` (`gid`, `p1_id`, `p2_id`, `seed`, `winner`) VALUES
(-1, 5, 4, 137, 4),
(69, 4, 5, 1337, 4),
(72, 4, 5, 137, 4),
(73, 4, 5, 137, 5),
(74, 5, 4, 137, 4),
(89, 4, 5, 137, 5),
(90, 5, 4, 137, 5),
(103, 5, 4, 137, 5),
(104, 5, 4, 137, 4),
(111, 4, 5, 137, 4),
(115, 4, 5, 137, 5),
(245, 4, 4, 137, 4),
(246, 4, 5, 137, 5),
(247, 4, 5, 137, 5),
(259, 4, 5, 137, 4),
(260, 4, 5, 137, 4),
(263, 5, 4, 137, 4),
(264, 5, 4, 137, 5),
(265, 4, 5, 137, 4);

-- --------------------------------------------------------

--
-- Table structure for table `players`
--

CREATE TABLE `players` (
  `pid` int(5) NOT NULL,
  `name` varchar(20) NOT NULL,
  `passwd` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `players`
--

INSERT INTO `players` (`pid`, `name`, `passwd`) VALUES
(4, 'asdf', 'xd'),
(5, 'asd', 'asd'),
(7, 'lol', 'kuk'),
(8, 'asdf2', 'xdd'),
(9, 'qfwefqwef', 'asdfasdf');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `games`
--
ALTER TABLE `games`
  ADD PRIMARY KEY (`gid`),
  ADD KEY `games_ibfk_1` (`p1_id`),
  ADD KEY `games_ibfk_2` (`p2_id`);

--
-- Indexes for table `games_history`
--
ALTER TABLE `games_history`
  ADD PRIMARY KEY (`gid`),
  ADD KEY `p1_id` (`p1_id`),
  ADD KEY `p2_id` (`p2_id`);

--
-- Indexes for table `players`
--
ALTER TABLE `players`
  ADD PRIMARY KEY (`pid`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `games`
--
ALTER TABLE `games`
  MODIFY `gid` int(8) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=269;

--
-- AUTO_INCREMENT for table `players`
--
ALTER TABLE `players`
  MODIFY `pid` int(5) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `games`
--
ALTER TABLE `games`
  ADD CONSTRAINT `games_ibfk_1` FOREIGN KEY (`p1_id`) REFERENCES `players` (`pid`),
  ADD CONSTRAINT `games_ibfk_2` FOREIGN KEY (`p2_id`) REFERENCES `players` (`pid`);

--
-- Constraints for table `games_history`
--
ALTER TABLE `games_history`
  ADD CONSTRAINT `games_history_ibfk_1` FOREIGN KEY (`p1_id`) REFERENCES `players` (`pid`),
  ADD CONSTRAINT `games_history_ibfk_2` FOREIGN KEY (`p2_id`) REFERENCES `players` (`pid`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
