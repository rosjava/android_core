cmake_minimum_required(VERSION 2.8.3)
project(android_core)

find_package(catkin REQUIRED rosjava_build_tools)

catkin_android_setup(assembleRelease uploadArchives)

catkin_package()

