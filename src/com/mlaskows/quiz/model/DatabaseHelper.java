/*
 * Copyright (c) 2013, Maciej Laskowski. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  
 * 
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact mlaskowsk@gmail.com if you need additional information
 * or have any questions.
 */

package com.mlaskows.quiz.model;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import android.content.Context;
import android.content.res.Resources;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.mlaskows.quiz.R;
import com.mlaskows.quiz.model.entities.Answer;
import com.mlaskows.quiz.model.entities.Exercise;
import com.mlaskows.quiz.model.entities.Level;
import com.mlaskows.quiz.model.entities.Question;
import com.mlaskows.quiz.model.entities.Quiz;
import com.mlaskows.quiz.model.entities.Scoring;

/**
 * Creates database in first application run. Also can
 * return DAO. To enable ORMLite logs execute 'setprop
 * log.tag.ORMLite DEBUG' on device
 * 
 * @author Maciej Laskowski
 * 
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

	/** Name of the database file for application */
	private static final String DATABASE_NAME = "quizes.sqlite";

	/**
	 * Any time changes are made to database objects, may
	 * have to increase the database version.
	 */
	private static final int DATABASE_VERSION = 1;

	/** Application context. */
	Context context;

	/** Level DAO */
	private Dao<Level, Integer> levelDao;

	/** Exercise DAO */
	private Dao<Exercise, Integer> exerciseDao;

	/** Question DAO */
	private Dao<Question, Integer> questionDao;

	/** Answer DAO */
	private Dao<Answer, Integer> answerDao;

	/** Scoring DAO */
	private Dao<Scoring, Integer> scoringDao;

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
	}

	/*
	 * (non-Javadoc)
	 * @see com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase,
	 * com.j256.ormlite.support.ConnectionSource)
	 */
	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource cs) {
		try {
			// Create DB
			TableUtils.createTable(cs, Level.class);
			TableUtils.createTable(cs, Exercise.class);
			TableUtils.createTable(cs, Question.class);
			TableUtils.createTable(cs, Answer.class);
			TableUtils.createTable(cs, Scoring.class);
			// Load content from XML to database. Children
			// first than parents.
			levelDao = new BaseDaoImpl<Level, Integer>(getConnectionSource(), Level.class) {
			};
			exerciseDao = new BaseDaoImpl<Exercise, Integer>(getConnectionSource(), Exercise.class) {
			};
			questionDao = new BaseDaoImpl<Question, Integer>(getConnectionSource(), Question.class) {
			};
			answerDao = new BaseDaoImpl<Answer, Integer>(getConnectionSource(), Answer.class) {
			};
			scoringDao = new BaseDaoImpl<Scoring, Integer>(getConnectionSource(), Scoring.class) {
			};
			Quiz quiz = loadXml();
			for (Level level : quiz.getLevels()) {
				for (Exercise exercise : level.getExercises()) {
					exercise.setLevel(level);
					questionDao.create(exercise.getQuestion());
					questionDao.refresh(exercise.getQuestion());
					exerciseDao.create(exercise);
					for (Answer answer : exercise.getAnswers()) {
						answer.setExercise(exercise);
						answerDao.create(answer);
					}
				}
				Scoring scoring = level.getScoring();
				scoring.setLevel(level);
				scoringDao.create(scoring);
				levelDao.create(level);
			}
		} catch (SQLException e) {
			Log.e(DATABASE_NAME, "Can't create database!", e);
			throw new RuntimeException(e);
		} catch (java.sql.SQLException e) {
			Log.e(DATABASE_NAME, "Error while creating DB!", e);
		} catch (Exception e) {
			Log.e(DatabaseHelper.class.getSimpleName(), "Cannot load XML to DB!", e);
		}

	}

	/*
	 * (non-Javadoc)
	 * @see com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase,
	 * com.j256.ormlite.support.ConnectionSource, int, int)
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource cs, int oldVersion, int newVersion) {
		try {
			List<String> allSql = new ArrayList<String>();
			// DB upgrade can depend on version is upgrading
			// from
			switch (oldVersion) {
			case 1:
				// allSql.add("SQL query 1 here");
				// allSql.add("SQL query 2 here");
			}
			for (String sql : allSql) {
				db.execSQL(sql);
			}
		} catch (SQLException e) {
			Log.e(DATABASE_NAME, "Exception during DB upgrade!", e);
			throw new RuntimeException(e);
		}

	}

	/**
	 * Return {@link Level} DAO to access DB.
	 * 
	 * @return Level DAO
	 */
	public Dao<Level, Integer> getLevelDao() {
		if (levelDao == null) {
			try {
				levelDao = new BaseDaoImpl<Level, Integer>(getConnectionSource(), Level.class) {
				};
			} catch (java.sql.SQLException e) {
				Log.e(DatabaseHelper.class.getSimpleName(), "Cannot create DAO!", e);
			}
		}
		return levelDao;
	}

	/**
	 * Return {@link Exercise} DAO to access DB.
	 * 
	 * @return Exercise DAO
	 */
	public Dao<Exercise, Integer> getExerciseDao() {
		if (exerciseDao == null) {
			try {
				exerciseDao = new BaseDaoImpl<Exercise, Integer>(getConnectionSource(), Exercise.class) {
				};
			} catch (java.sql.SQLException e) {
				Log.e(DatabaseHelper.class.getSimpleName(), "Cannot create DAO!", e);
			}
		}
		return exerciseDao;
	}

	/**
	 * Return {@link Scoring} DAO to access DB.
	 * 
	 * @return Scoring DAO
	 */
	public Dao<Scoring, Integer> getScoringDao() {
		if (scoringDao == null) {
			try {
				scoringDao = new BaseDaoImpl<Scoring, Integer>(getConnectionSource(), Scoring.class) {
				};
			} catch (java.sql.SQLException e) {
				Log.e(DatabaseHelper.class.getSimpleName(), "Cannot create DAO!", e);
			}
		}
		return scoringDao;
	}

	/**
	 * Loads XML with quiz and returns {@link Quiz} object.
	 * 
	 * @return quiz object
	 * @throws Exception
	 *             when deserialization fails
	 */
	private Quiz loadXml() throws Exception {
		// Get resources
		Resources resources = context.getResources();
		// Determine locale
		Locale locale = resources.getConfiguration().locale;
		String code = locale.getLanguage();
		// Get XML name using reflection
		Field field = null;
		String prefix = context.getString(R.string.xml_prefix);
		try {
			field = R.raw.class.getField(prefix + code);
		} catch (NoSuchFieldException e) {
			// If there is no language available use default
			field = R.raw.class.getField(prefix + context.getString(R.string.default_language));
		}
		// Create InputSream from XML resource
		InputStream source = resources.openRawResource(field.getInt(null));
		// Parse XML
		Serializer serializer = new Persister();
		return serializer.read(Quiz.class, source);
	}

}
