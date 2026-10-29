package com.space.space_bundle.repository;

import com.space.space_bundle.entity.SmsOperationsSettings;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SmsOperationsSettingsRepository extends MongoRepository<SmsOperationsSettings, String> {}
