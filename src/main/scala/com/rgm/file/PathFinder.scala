package com.rgm.file

trait PathFinder {
  def +++(includes: PathFinder): PathFinder

  def ---(excludes: PathFinder): PathFinder

  def *(matcher: PathMatcher): PathFinder

  def **(matcher: PathMatcher): PathFinder

  def *** : PathFinder

  def /(literal: String): PathFinder

  def get: Iterable[Path]
}