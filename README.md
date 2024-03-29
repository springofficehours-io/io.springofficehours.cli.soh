# Spring Office Hours Helper CLI

This is a CLI for updating [Spring Office Hours](https://bit.ly/spring-office-hours) episode metadata

## Quick Start

```bash
git clone https://github.com/vmware-tanzu/tanzu-dev-portal.git
cd tanzu-dev-portal
git checkout -b episode1000
show --episodeNumber 1000 --title "S3E1 - Spring Into The New Year" --date "2023-01-02" --youTubeId "9zUaIiI47nc"
```
Shows the options for the command:
```text
NAME
episode-images - create episode image from template and text

SYNOPSIS
episode-images --text String --path String

OPTIONS
--text String
[Optional, default = Episode 0000]

--path String
[Optional, default = ./content/tv/spring-office-hours/example.png]
```
Run the command:
```bash
docker run \
-it -v ${PWD}/content:/workspace/content \
dashaun/spring-office-hours-helper:latest \
episode-images "episode 1000: Celebrating a milestone" ./content/tv/spring-office-hours/1000/images/1000.png
```
> You can set an ALIAS for the docker command

The resulting image will look like this:
![Example](docs/1000.png)


## Background

- Started developing this with Spring Boot 2.7 and Spring-Native.
- It uses javax.imageio.ImageIO, adding the static call to scan for plugins fixed my AOT processing issues
- It uses java.awt.* which doesn't work, yet, with GraalVM on macOS. So I decided to create a docker image (linux) that can be used on Mac, Linux, Windows, to work around that limitation.
- Using java.awt.* requires the use of the "full" builder.

## TODO
1. Command line options to consider:
- Episode number
- Episode Title
- Guest
- Date
- Time
- Description

2. Make the docker run cleaner, more like CLI experience, hide crufty logs