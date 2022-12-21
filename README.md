# Spring Office Hours Helper CLI

This is a CLI for updating [Spring Office Hours](https://bit.ly/spring-office-hours) episode metadata

## Quick Start

```bash
git clone https://github.com/vmware-tanzu/tanzu-dev-portal.git
cd tanzu-dev-portal
git checkout -b episode1000
hugo new --kind soh tv/spring-office-hours/1000
docker run -it -v ${PWD}/content:/workspace/content dashaun/spring-office-hours-helper:latest help episode-images
```
>NAME
>episode-images - create episode image from template and text
>
>SYNOPSIS
>episode-images --text String --path String
>
>OPTIONS
>--text String
>[Optional, default = Episode 0000]
>
>       --path String
>       [Optional, default = ./content/tv/spring-office-hours/example.png]

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

Started developing this with Spring Boot 2.7 and Spring-Native.

###
It uses javax.imageio.ImageIO
> Adding the static call to scan for plugins fixed my AOT processing issues

###
It uses java.awt.* which doesn't work, yet, with GraalVM on macOS. So I decided to create a docker image (linux) that can be used on Mac, Linux, Windows, to work around that limitation.

Using java.awt.* requires the use of the "full" builder.

## TODO
Command line options to consider:
- Episode number
- Episode Title
- Guest
- Date
- Time
- Description

Make the docker run cleaner, more like CLI experience, hide crufty logs